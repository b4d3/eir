import cats.effect.{ExitCode, IO, IOApp, Sync}
import com.typesafe.scalalogging.Logger
import eu.timepit.refined.auto._
import messages.CheckImei
import traffic.protocols.ZmqProtocol
import utils.logging.Logging
import utils.logging.interpreters.IOLogger
import cats.implicits._

object Main extends IOApp {

  private implicit val logger: Logger = Logger("Traffgen Main")

  private val imei = CheckImei("1234567890123400")
  private val unknownImei = CheckImei("1234567890123400")
  private val blacklistedImei = CheckImei("1234567890123456")

  def program[F[_] : Sync : Logging]: F[Unit] =
    for {
      zmqProtocol <- ZmqProtocol[F]()
      traffgen <- Sync[F].delay(new TrafficGenerator[F](zmqProtocol))
      _ <- traffgen.sendCheckImeiMessage(imei) >>= (s => Logging[F].debug(s))
      _ <- traffgen.sendCheckImeiMessage(unknownImei) >>= (s => Logging[F].debug(s))
      _ <- traffgen.sendCheckImeiMessage(blacklistedImei) >>= (s => Logging[F].debug(s))
    } yield ()

  def run(args: List[String]): IO[ExitCode] = {

    implicit val logging: Logging[IO] = IOLogger.ioLogger

    program[IO].attempt.map(_.fold(_ => 1, _ => 0)).map(ExitCode(_))
  }
}