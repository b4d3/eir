package traffic.protocols

import java.math.BigInteger

import org.zeromq.{ZContext, ZFrame, ZMQ, ZMsg}
import responseColors.ResponseColor

trait ZmqProtocol extends Protocol {

  val context: ZContext = new ZContext(1)

  val frontendSocket: ZMQ.Socket = context.createSocket(ZMQ.ROUTER)
  frontendSocket.bind("tcp://*:5555")

  val requestQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PUSH)
  requestQueueSocket.bind("inproc://requestQueueSocket")

  val responseQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PULL)
  responseQueueSocket.bind("inproc://responseQueueSocket")


  val inprocRequestQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PULL)
  inprocRequestQueueSocket.connect("inproc://requestQueueSocket")

  val inprocResponseQueueSocket: ZMQ.Socket = context.createSocket(ZMQ.PUSH)
  inprocResponseQueueSocket.connect("inproc://responseQueueSocket")

  new Thread(() => {

    println("Started receiving messages")

    while (true) {

      val msg = ZMsg.recvMsg(frontendSocket)

      // Message from client's REQ socket contains 3 frames:
      // address + empty frame + request content (payload)
      val reqAddress = msg.pop
      val emptyFrame = msg.pop
      val reqPayload = msg.pop

      assert(reqPayload != null)
      msg.destroy()

      println(s"RECEIVED: $reqPayload FROM: $reqAddress")

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

  override def sendMessage(address: String, responseColor: ResponseColor.Value): Unit = {

    inprocResponseQueueSocket.send(s"$address;${responseColor.toString}")
  }
}
