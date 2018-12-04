package repository.repositories

import messages.{CheckImei, CheckImeiMessage, Imei}
import repository.EirRepository
import eu.timepit.refined.auto._

trait InMemoryRepository extends EirRepository {

  val blacklistedImeis: Set[Imei] = Set("1234567890123456", "1234567890123457", "1234567890123458")

  override def getResponseColor(checkImeiMessage: CheckImeiMessage): String = checkImeiMessage

  match {
    case CheckImei(imei) if blacklistedImeis.contains(imei) => "BLACK"
    case _ => "WHITE"
  }
}
