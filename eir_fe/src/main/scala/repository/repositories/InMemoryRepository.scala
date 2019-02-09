package repository.repositories

import cats.Applicative
import cats.implicits._
import messages.{CheckImei, CheckImeiMessage, Imei}
import repository.EirRepository
import eu.timepit.refined.auto._

final class InMemoryRepository[F[_] : Applicative] extends EirRepository[F] {

  val blacklistedImeis: Set[Imei] = Set("1234567890123456", "1234567890123457", "1234567890123458")

  override def getResponseColor(checkImeiMessage: CheckImeiMessage): F[String] =
    (checkImeiMessage match {
      case CheckImei(imei) if blacklistedImeis.contains(imei) => "BLACK"
      case _ => "WHITE"
    }).pure[F]
}
