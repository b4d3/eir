import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import eu.timepit.refined.auto._
import messages.CheckImei
import traffic.protocols.ZmqProtocol
import utils.logging.Logging
import utils.logging.interpreters.IOLogger

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  private implicit val logger: Logger = Logger("Traffgen Main")
  // Needed for `start`
  implicit val ctx = IO.contextShift(global)

  private val imei = CheckImei("1234567890123400")
  private val unknownImei = CheckImei("1234567890123400")
  private val blacklistedImei = CheckImei("1234567890123456")

  def program: IO[Unit] = {
    implicit val logging: Logging[IO] = IOLogger.ioLogger
    for {
      zmqProtocol <- ZmqProtocol[IO]()
      traffgen <- Sync[IO].delay(new TrafficGenerator[IO](zmqProtocol))
      _ <- (traffgen.sendCheckImeiMessage(imei) >>= (Logging[IO].debug(_))).foreverM.start
      _ <- (traffgen.sendCheckImeiMessage(unknownImei) >>= (Logging[IO].debug(_))).foreverM.start
      _ <- (traffgen.sendCheckImeiMessage(blacklistedImei) >>= (Logging[IO].debug(_))).foreverM.start
      _ <- IO.never
    } yield ()
  }

  def run(args: List[String]): IO[ExitCode] = program.attempt.map(_.fold(_ => 1, _ => 0)).map(ExitCode(_))
}