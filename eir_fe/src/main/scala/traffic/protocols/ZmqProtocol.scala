package traffic.protocols

import java.math.BigInteger

import com.typesafe.scalalogging.Logger
import config.{EirFeConfig, FmConfig}
import org.zeromq.{ZContext, ZFrame, ZMQ, ZMsg}
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import responseColors.ResponseColor
import pureconfig.generic.auto._

trait ZmqProtocol extends Protocol {

  private val logger = Logger(classOf[ZmqProtocol])
  implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))
  private val config = pureconfig.loadConfigOrThrow[EirFeConfig]

  private val context: ZContext = new ZContext(1)

  private val frontendSocket: ZMQ.Socket = context.createSocket(ZMQ.ROUTER)

  {
    val protocol = config.feEndpoint.protocol
    val address = config.feEndpoint.address
    val port = config.feEndpoint.port
    frontendSocket.bind(s"$protocol$address:$port")
  }

  private val requestQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PUSH)
  requestQueueSocket.bind("inproc://requestQueueSocket")

  private val responseQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PULL)
  responseQueueSocket.bind("inproc://responseQueueSocket")

  private val inprocRequestQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PULL)
  inprocRequestQueueSocket.connect("inproc://requestQueueSocket")

  private val inprocResponseQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PUSH)
  inprocResponseQueueSocket.connect("inproc://responseQueueSocket")

  new Thread(() => {

    logger.info("Started receiving messages")

    while (true) {

      val msg = ZMsg.recvMsg(frontendSocket)

      // Message from client's REQ socket contains 3 frames:
      // address + empty frame + request content (payload)
      val reqAddress = msg.pop
      val emptyFrame = msg.pop
      val reqPayload = msg.pop

      assert(reqPayload != null)
      msg.destroy()

      logger.debug(s"RECEIVED: $reqPayload FROM: $reqAddress")

      requestQueueSocket.send(s"$reqAddress;$reqPayload")

      val responseMessage = new String(responseQueueSocket.recv(0))

      val (respAddress: String, respPayload: String) = extractAddressAndPayload(responseMessage)

      sendToClient(respAddress, respPayload)

    }
  }).start()


  private def sendToClient(respAddress: String, respPayload: String): Unit = {

    val addressByteArray = new BigInteger(respAddress, 16).toByteArray
    val respAddressFrame = new ZFrame(addressByteArray)
    val respEmptyFrame = new ZFrame("")
    val respPayloadFrame = new ZFrame(respPayload)

    respAddressFrame.send(frontendSocket, ZFrame.REUSE + ZFrame.MORE)
    // Sending empty frame because client expects such constructed message
    respEmptyFrame.send(frontendSocket, ZFrame.REUSE + ZFrame.MORE)
    respPayloadFrame.send(frontendSocket, ZFrame.REUSE)

    respAddressFrame.destroy()
    respEmptyFrame.destroy()
    respPayloadFrame.destroy()
  }

  private def extractAddressAndPayload(responseMessage: String): (String, String) = {

    val respMessageSplit = responseMessage.split(";")

    val respAddress = respMessageSplit(0)
    val respPayload = respMessageSplit(1)
    (respAddress, respPayload)
  }

  override def receiveMessage(): (String, String) = {

    val message = new String(inprocRequestQueueSocket.recv(0))

    val (address: String, payload: String) = extractAddressAndPayload(message)

    (address, payload)
  }

  override def sendMessage(address: String, responseColor: ResponseColor): Unit = {

    inprocResponseQueueSocket.send(s"$address;${responseColor.toString}")
  }
}
