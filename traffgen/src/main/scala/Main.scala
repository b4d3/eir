import com.typesafe.scalalogging.Logger
import eu.timepit.refined.auto._
import messages.CheckImei
import traffic.protocols.ZmqProtocol

object Main extends App {

  val logger = Logger("TrafficGenerator Main")

  val imei = CheckImei("1234567890123400")
  val unknownImei = CheckImei("1234567890123400")
  val blacklistedImei = CheckImei("1234567890123456")

  val trafficGenerator = new TrafficGenerator with ZmqProtocol

  while (true) {
    logger.debug(trafficGenerator.sendCheckImeiMessage(imei))
    Thread.sleep(100)
    logger.debug(trafficGenerator.sendCheckImeiMessage(unknownImei))
    Thread.sleep(100)
    logger.debug(trafficGenerator.sendCheckImeiMessage(blacklistedImei))
  }

}