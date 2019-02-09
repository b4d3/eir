package traffic.protocols

import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import pureconfig.generic.auto._
import config.FeEndpoint
import org.zeromq.ZMQ
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import utils.logging.Logging
import cats.implicits._

object ZmqProtocol {

  def apply[F[_] : Sync : Logging](): F[ZmqProtocol[F]] = {

    implicit val className: Logger = Logger(classOf[ZmqProtocol[F]])

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val config = pureconfig.loadConfigOrThrow[FeEndpoint]("feEndpoint")

    for {
      socket <- Sync[F].delay(ZMQ.context(1)).map(_.socket(ZMQ.REQ))
      _ <- Logging[F].info("Connecting to EIR")
      connected <- Sync[F].delay(socket.connect(s"${config.protocol}${config.address}:${config.port}"))
      _ <- if (connected) Logging[F].info("Connected!") else Logging[F].info("Not connected!")
    } yield new ZmqProtocol[F](socket)

  }
}

class ZmqProtocol[F[_] : Sync : Logging] private(socket: ZMQ.Socket) extends Protocol[F] {

  private implicit val className: Logger = Logger(classOf[ZmqProtocol[F]])

  override def sendAndReceiveResp(message: String): F[String] =
    for {
      _ <- Logging[F].debug(s"Sending request $message")
      _ <- Sync[F].delay(socket.send(message.getBytes(), 0))
      reply <- Sync[F].delay(socket.recv(0))
    } yield new String(s"$message=${new String(reply)}")
}