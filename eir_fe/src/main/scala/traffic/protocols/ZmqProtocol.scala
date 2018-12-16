package traffic.protocols

import java.math.BigInteger

import com.typesafe.scalalogging.Logger
import config.EirFeConfig
import org.zeromq.ZMQ.Socket
import org.zeromq.{ZContext, ZFrame, ZMQ, ZMsg}
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import responseColors.ResponseColor
import pureconfig.generic.auto._
import scalaz.Monad
import scalaz.Scalaz._
import utils.logging.Logging

object ZmqProtocol {

  def apply[F[_] : Monad : Logging]: F[ZmqProtocol[F]] = {

    implicit val logger = Logger(classOf[ZmqProtocol[F]])

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val config = pureconfig.loadConfigOrThrow[EirFeConfig]

    val protocol = config.feEndpoint.protocol
    val address = config.feEndpoint.address
    val port = config.feEndpoint.port

    for {
      context <- Monad[F].pure(new ZContext(1))
      frontendSocket = context.createSocket(ZMQ.ROUTER)
      requestQueueSocket = context.createSocket(ZMQ.PUSH)
      responseQueueSocket = context.createSocket(ZMQ.PULL)
      inprocRequestQueueSocket = context.createSocket(ZMQ.PULL)
      inprocResponseQueueSocket = context.createSocket(ZMQ.PUSH)

      _ <- Monad[F].pure(frontendSocket.bind(s"$protocol$address:$port"))
      _ <- Monad[F].pure(requestQueueSocket.bind("inproc://requestQueueSocket"))
      _ <- Monad[F].pure(responseQueueSocket.bind("inproc://responseQueueSocket"))
      _ <- Monad[F].pure(inprocRequestQueueSocket.connect("inproc://requestQueueSocket"))
      _ <- Monad[F].pure(inprocResponseQueueSocket.connect("inproc://responseQueueSocket"))

    } yield new ZmqProtocol[F](frontendSocket, requestQueueSocket, responseQueueSocket,
      inprocRequestQueueSocket, inprocResponseQueueSocket)
  }
}

class ZmqProtocol[F[_] : Monad : Logging] private(frontendSocket: Socket,
                                                  requestQueueSocket: Socket,
                                                  responseQueueSocket: Socket,
                                                  inprocRequestQueueSocket: Socket,
                                                  inprocResponseQueueSocket: Socket)
  extends Protocol[F] {

  implicit private val logger = Logger(classOf[ZmqProtocol[F]])

  new Thread(() => {

    Logging[F].info("Started receiving messages")

    while (true) {

      val msg = ZMsg.recvMsg(frontendSocket)

      // Message from client's REQ socket contains 3 frames:
      // address + empty frame + request content (payload)
      val reqAddress = msg.pop
      val emptyFrame = msg.pop
      val reqPayload = msg.pop

      assert(reqPayload != null)
      msg.destroy()

      Logging[F].debug(s"RECEIVED: $reqPayload FROM: $reqAddress")

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

  override def receiveMessage(): F[(String, String)] =
    for {
      message <- Monad[F].pure(new String(inprocRequestQueueSocket.recv(0)))
      (address, payload) = extractAddressAndPayload(message)
    } yield (address, payload)

  override def sendMessage(address: String, responseColor: ResponseColor): F[Unit] =
    for {
      _ <- Monad[F].pure(inprocResponseQueueSocket.send(s"$address;${responseColor.toString}"))
    } yield ()
}
