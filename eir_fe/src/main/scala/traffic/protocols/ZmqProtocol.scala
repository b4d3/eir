package traffic.protocols

import java.math.BigInteger

import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.Logger
import config.EirFeConfig
import org.zeromq.ZMQ.Socket
import org.zeromq.{ZContext, ZFrame, ZMQ, ZMsg}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.{CamelCase, ConfigFieldMapping}
import responseColors.ResponseColor
import utils.logging.Logging

object ZmqProtocol {

  def apply[F[_] : Sync : Logging]: F[ZmqProtocol[F]] = {

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val config = pureconfig.loadConfigOrThrow[EirFeConfig]

    val protocol = config.feEndpoint.protocol
    val address = config.feEndpoint.address
    val port = config.feEndpoint.port

    for {
      context <- Sync[F].delay(new ZContext(1))
      frontendSocket = context.createSocket(ZMQ.ROUTER)
      requestQueueSocket = context.createSocket(ZMQ.PUSH)
      responseQueueSocket = context.createSocket(ZMQ.PULL)
      inprocRequestQueueSocket = context.createSocket(ZMQ.PULL)
      inprocResponseQueueSocket = context.createSocket(ZMQ.PUSH)

      _ <- Sync[F].delay(frontendSocket.bind(s"$protocol$address:$port"))
      _ <- Sync[F].delay(requestQueueSocket.bind("inproc://requestQueueSocket"))
      _ <- Sync[F].delay(responseQueueSocket.bind("inproc://responseQueueSocket"))
      _ <- Sync[F].delay(inprocRequestQueueSocket.connect("inproc://requestQueueSocket"))
      _ <- Sync[F].delay(inprocResponseQueueSocket.connect("inproc://responseQueueSocket"))

    } yield new ZmqProtocol[F](frontendSocket, requestQueueSocket, responseQueueSocket,
      inprocRequestQueueSocket, inprocResponseQueueSocket)
  }
}

final class ZmqProtocol[F[_] : Sync : Logging] private(frontendSocket: Socket,
                                                       requestQueueSocket: Socket,
                                                       responseQueueSocket: Socket,
                                                       inprocRequestQueueSocket: Socket,
                                                       inprocResponseQueueSocket: Socket)
  extends Protocol[F] {

  implicit private val logger: Logger = Logger(classOf[ZmqProtocol[F]])

  private def sendToClient(respAddress: String, respPayload: String): F[Unit] = for {

    addressByteArray <- Sync[F].delay(new BigInteger(respAddress, 16)).map(_.toByteArray)
    respAddressFrame <- Sync[F].delay(new ZFrame(addressByteArray))
    respEmptyFrame <- Sync[F].delay(new ZFrame(""))
    respPayloadFrame <- Sync[F].delay(new ZFrame(respPayload))

    _ <- Sync[F].delay(respAddressFrame.send(frontendSocket, ZFrame.REUSE + ZFrame.MORE))
    // Sending empty frame because client expects such constructed message
    _ <- Sync[F].delay(respEmptyFrame.send(frontendSocket, ZFrame.REUSE + ZFrame.MORE))
    _ <- Sync[F].delay(respPayloadFrame.send(frontendSocket, ZFrame.REUSE))

    _ <- Sync[F].delay(respAddressFrame.destroy())
    _ <- Sync[F].delay(respEmptyFrame.destroy())
    _ <- Sync[F].delay(respPayloadFrame.destroy())

  } yield ()

  private def extractAddressAndPayloadFromZFrame(msg: ZMsg): (String, String) = {
    // Message from client's REQ socket contains 3 frames:
    // address + empty frame + request content (payload)
    val reqAddress = msg.pop
    val emptyFrame = msg.pop
    val reqPayload = msg.pop

    // TODO In future, don't throw exception. Smarter error handling?
    assert(reqPayload != null)
    msg.destroy()

    (reqAddress.toString, reqPayload.toString)
  }

  override def receiveMessage(): F[(String, String)] =
    for {
      msg <- Sync[F].delay(ZMsg.recvMsg(frontendSocket))
      (address, payload) = extractAddressAndPayloadFromZFrame(msg)
      _ <- Logging[F].debug(s"RECEIVED: $payload FROM: $address")
    } yield (address, payload)

  override def sendMessage(address: String, responseColor: ResponseColor): F[Unit] =
    sendToClient(address, responseColor.toString)
}
