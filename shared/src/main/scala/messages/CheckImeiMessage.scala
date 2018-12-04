package messages

sealed abstract class CheckImeiMessage

final case class CheckImei(imei: Imei) extends CheckImeiMessage

final case class CheckImeiWithImsi(imei: Imei, imsi: Imsi)
  extends CheckImeiMessage