import com.typesafe.scalalogging.Logger
import messages.{CheckImei, Imei}
import traffic.protocols.ZmqProtocol
import eu.timepit.refined.api.RefType
import eu.timepit.refined.boolean._
import eu.timepit.refined.char._
import eu.timepit.refined.collection._
import eu.timepit.refined.generic._
import eu.timepit.refined.string._
import shapeless.{::, HNil}
import eu.timepit.refined.auto._
import eu.timepit.refined._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.Positive

object Main extends App {

  val logger = Logger("TrafficGenerator Main")

//  val im: Imei =
  val imei = CheckImei("1234567890123400")
//  val unknownImei = CheckImei("1234567890123400")
//  val blacklistedImei = CheckImei("1234567890123456")

  val trafficGenerator = new TrafficGenerator with ZmqProtocol

  while (true) {
    logger.debug(trafficGenerator.sendCheckImeiMessage(imei))
    Thread.sleep(100)
//    logger.debug(trafficGenerator.sendCheckImeiMessage(unknownImei))
    Thread.sleep(100)
//    logger.debug(trafficGenerator.sendCheckImeiMessage(blacklistedImei))
  }

}