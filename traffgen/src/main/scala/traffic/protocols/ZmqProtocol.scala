package traffic.protocols

import com.typesafe.scalalogging.Logger
import pureconfig.generic.auto._
import config.FeEndpoint
import org.zeromq.ZMQ
import pureconfig.generic.ProductHint
import pureconfig.{CamelCase, ConfigFieldMapping}
import scalaz.Monad
import scalaz.Scalaz._
import utils.logging.Logging

object ZmqProtocol {

  def apply[F[_] : Monad : Logging](): F[ZmqProtocol[F]] = {

    implicit val className: Logger = Logger(classOf[ZmqProtocol[F]])

    implicit def hint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

    val config = pureconfig.loadConfigOrThrow[FeEndpoint]("feEndpoint")

    for {
      socket <- Monad[F].pure(ZMQ.context(1)).map(_.socket(ZMQ.REQ))
      _ <- Logging[F].info("Connecting to EIR")
      connected <- Monad[F].pure(socket.connect(s"${config.protocol}${config.address}:${config.port}"))
      _ <- if (connected) Logging[F].info("Connected!") else Logging[F].info("Not connected!")
    } yield new ZmqProtocol[F](socket)

  }
}

class ZmqProtocol[F[_] : Monad : Logging] private(socket: ZMQ.Socket) extends Protocol[F] {

  private implicit val className: Logger = Logger(classOf[ZmqProtocol[F]])

  override def send(message: String): F[String] =
    for {
      _ <- Logging[F].debug(s"Sending request $message")
      _ <- Monad[F].pure(socket.send(message.getBytes(), 0))
      reply <- Monad[F].pure(socket.recv(0))
    } yield new String(s"$message=${new String(reply)}")
}