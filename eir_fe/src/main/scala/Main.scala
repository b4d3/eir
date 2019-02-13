import java.util.concurrent.LinkedBlockingQueue

import cats.effect._
import cats.implicits._
import com.typesafe.scalalogging.Logger
import faultManagement.{AlarmThrottling, FaultManager, LoggingNotifier}
import messages.CheckImeiMessage
import repository.EirRepositoryHandler
import repository.repositories.LdapRepository
import responseColors.ResponseColor
import traffic.handler.TrafficHandler
import traffic.protocols.ZmqProtocol
import utils.logging.Logging
import utils.logging.interpreters.IOLogger

import scala.concurrent.ExecutionContext.Implicits.global

object Main extends IOApp {

  // Needed for `start`
  implicit val ctx = IO.contextShift(global)

  private implicit val logger = Logger("Eir Main")

  def programDeps[F[_] : Sync : Logging : AlarmThrottling : Timer]: F[(TrafficHandler[F], EirRepositoryHandler[F])] =
    for {
      _ <- Logging[F].info("Starting EIR node")
      checkImeiRequestQueue = new LinkedBlockingQueue[(String, CheckImeiMessage)]
      checkImeiResponseQueue = new LinkedBlockingQueue[(String, ResponseColor)]
      zmqProtocol <- ZmqProtocol[F]
      trafficHandler <- TrafficHandler(zmqProtocol, checkImeiRequestQueue, checkImeiResponseQueue)
      notifier <- Sync[F].delay(new LoggingNotifier[F])
      faultManager <- Sync[F].delay(new FaultManager[F](notifier))
      ldapRepository <- LdapRepository[F](faultManager)
      repositoryHandler <- EirRepositoryHandler(ldapRepository, checkImeiRequestQueue, checkImeiResponseQueue)

    } yield (trafficHandler, repositoryHandler)

  def run(args: List[String]): IO[ExitCode] = {

    implicit val logging: Logging[IO] = IOLogger.ioLogger

    (for {
      tr <- AlarmThrottling.create[IO]().flatMap { at =>
        implicit val alarmThrottling: AlarmThrottling[IO] = at
        programDeps[IO]
      }
      trafficHandler = tr._1
      repositoryHandler = tr._2

      inTraffWorker = trafficHandler.handleIncomingMessage().foreverM
      outTraffWorker = trafficHandler.handleOutgoingMessage().foreverM
      eirRepoWorker = repositoryHandler.handleEirRepositoryMessage().foreverM
      _ <- List.fill(1000)(inTraffWorker).traverse(_.start)
      _ <- List.fill(1000)(outTraffWorker).traverse(_.start)
      _ <- List.fill(1000)(eirRepoWorker).traverse(_.start)
      _ <- IO.never
    } yield ()).attempt.map(_.fold(_ => 1, _ => 0)).map(ExitCode(_))
  }
}
