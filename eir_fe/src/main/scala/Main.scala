import java.util.concurrent.LinkedBlockingQueue

import com.typesafe.scalalogging.Logger
import faultManagement.{FaultManager, LoggingFaultManager}
import messages.CheckImeiMessage
import repository.EirRepositoryHandler
import repository.repositories.LdapRepository
import responseColors.ResponseColor
import traffic.handler.TrafficHandler
import traffic.protocols.ZmqProtocol

object Main extends App {

  val logger = Logger("Eir Main")

  logger.info("Starting EIR node")

  val checkImeiRequestQueue = new LinkedBlockingQueue[(String, CheckImeiMessage)]
  val checkImeiResponseQueue = new LinkedBlockingQueue[(String, ResponseColor.Value)]

  val trafficHandler = new TrafficHandler(checkImeiRequestQueue, checkImeiResponseQueue) with
    ZmqProtocol

  val repositoryHandler = new EirRepositoryHandler(checkImeiRequestQueue, checkImeiResponseQueue)
    with LdapRepository {
    override lazy val faultManager: FaultManager = new LoggingFaultManager
  }

  trafficHandler.startHandlingTraffic()
}
