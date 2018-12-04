import messages._
import traffic.protocols.Protocol

abstract class TrafficGenerator extends Protocol {

  def sendCheckImeiMessage(checkImeiMessage: CheckImeiMessage): String = {

    val message = checkImeiMessage match {
      case CheckImei(imei) => imei.value
      case CheckImeiWithImsi(imei, imsi) => s"${imei.value};${imsi.value}"
    }

    send(message)
  }

}
