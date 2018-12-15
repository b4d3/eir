import messages._
import traffic.protocols.Protocol

final class TrafficGenerator[F[_]](protocol: Protocol[F]) {

  def sendCheckImeiMessage(checkImeiMessage: CheckImeiMessage): F[String] = {

    val message = checkImeiMessage match {
      case CheckImei(imei) => imei.value
      case CheckImeiWithImsi(imei, imsi) => s"${imei.value};${imsi.value}"
    }

    protocol.send(message)
  }
}
