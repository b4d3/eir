import java.util.concurrent.LinkedBlockingQueue

import messages.CheckImeiMessage
import repository.EirRepositoryHandler
import repository.repositories.LdapRepository
import responseColors.ResponseColor
import traffic.handler.TrafficHandler
import traffic.protocols.ZmqProtocol

object Main extends App {

  println("Starting EIR node")

  val checkImeiRequestQueue = new LinkedBlockingQueue[(String, CheckImeiMessage)]
  val checkImeiResponseQueue = new LinkedBlockingQueue[(String, ResponseColor.Value)]

  val trafficHandler = new TrafficHandler(checkImeiRequestQueue, checkImeiResponseQueue) with
    ZmqProtocol

  val repositoryHandler = new EirRepositoryHandler(checkImeiRequestQueue, checkImeiResponseQueue)
    with LdapRepository

  trafficHandler.startHandlingTraffic()
}
