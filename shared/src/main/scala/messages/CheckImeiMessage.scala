package messages

sealed abstract class CheckImeiMessage

case class CheckImei(imei: Imei) extends CheckImeiMessage

case class CheckImeiWithImsi(imei: Imei, imsi: Imsi) extends CheckImeiMessage