import com.typesafe.scalalogging.Logger
import eu.timepit.refined.auto._
import messages.CheckImei
import scalaz.Monad
import scalaz.Scalaz._
import scalaz.zio.IO
import traffic.protocols.ZmqProtocol
import utils.logging.Logging
import utils.logging.interpreters.IOLogger

object Main extends scalaz.zio.App {

  private implicit val logger: Logger = Logger("Traffgen Main")

  private val imei = CheckImei("1234567890123400")
  private val unknownImei = CheckImei("1234567890123400")
  private val blacklistedImei = CheckImei("1234567890123456")

  def program[F[_] : Monad : Logging]: F[Unit] =
    for {
      zmqProtocol <- ZmqProtocol[F]()
      traffgen <- Monad[F].pure(new TrafficGenerator[F](zmqProtocol))
      _ <- traffgen.sendCheckImeiMessage(imei) >>= (s => Logging[F].debug(s))
      _ <- traffgen.sendCheckImeiMessage(unknownImei) >>= (s => Logging[F].debug(s))
      _ <- traffgen.sendCheckImeiMessage(blacklistedImei) >>= (s => Logging[F].debug(s))
    } yield ()

  def run(args: List[String]): IO[Nothing, ExitStatus] = {

    implicit val logging = IOLogger.ioLogger
    implicit val monad = new Monad[IO[Nothing, ?]] {
      override def point[A](a: => A): IO[Nothing, A] = IO.sync(a)

      override def bind[A, B](fa: IO[Nothing, A])(f: A => IO[Nothing, B]): IO[Nothing, B] =
        fa.flatMap(f)
    }

    program[IO[Nothing, ?]].attempt.map(_.fold(_ => 1, _ => 0)).map(ExitStatus.ExitNow(_))
  }
}