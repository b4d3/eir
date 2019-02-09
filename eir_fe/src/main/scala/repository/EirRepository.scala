package repository

import messages.CheckImeiMessage

trait EirRepository[F[_]] {

  def getResponseColor(checkImeiMessage: CheckImeiMessage): F[String]
}
