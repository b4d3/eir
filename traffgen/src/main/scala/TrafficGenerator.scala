import messages._
import traffgen.protocols.Protocol

abstract class TrafficGenerator extends Protocol {

  def sendCheckImeiMessage(checkImeiMessage: CheckImeiMessage): String = {

    val message = checkImeiMessage match {
      case CheckImei(Imei(imei)) => imei
      case CheckImeiWithImsi(Imei(imei), Imsi(imsi)) => s"$imei;$imsi"
    }

    send(message)
  }

}
