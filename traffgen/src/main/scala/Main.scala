import messages.{CheckImei, Imei}
import traffgen.protocols.ZmqProtocol

object Main extends App {

  val imei = CheckImei(Imei("8884567890123456"))
  val unknownImei = CheckImei(Imei("1234567890123400"))
  val blacklistedImei = CheckImei(Imei("1234567890123456"))

  val trafficGenerator = new TrafficGenerator with ZmqProtocol

  while (true) {
    println(trafficGenerator.sendCheckImeiMessage(imei))
    Thread.sleep(100)
    println(trafficGenerator.sendCheckImeiMessage(unknownImei))
    Thread.sleep(100)
    println(trafficGenerator.sendCheckImeiMessage(blacklistedImei))
  }

}