package repository

import java.util.concurrent.LinkedBlockingQueue

import cats.implicits._
import cats.effect.Sync
import com.typesafe.scalalogging.Logger
import messages.CheckImeiMessage
import responseColors._

object EirRepositoryHandler {

  def apply[F[_] : Sync](eirRepository: EirRepository[F],
                         checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                         checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)])
  : F[EirRepositoryHandler[F]] =
    Sync[F].delay(new EirRepositoryHandler[F](eirRepository, checkImeiRequestQueue, checkImeiResponseQueue))
}

final class EirRepositoryHandler[F[_] : Sync] private(eirRepository: EirRepository[F],
                                                      val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                                                      val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)]) {

  private implicit val logger = Logger(classOf[EirRepositoryHandler[F]])

  def handleEirRepositoryMessage(): F[Unit] = for {

    addressAndCheckImeiMsg <- Sync[F].delay(checkImeiRequestQueue.take())
    (address, checkImeiMessage) = addressAndCheckImeiMsg
    responseColorString <- eirRepository.getResponseColor(checkImeiMessage)
    responseColor = responseColorString match {
      case "WHITE" => White()
      case "BLACK" => Black()
    }
    _ <- Sync[F].delay(checkImeiResponseQueue.put((address, responseColor)))
  } yield ()

}
