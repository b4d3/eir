package traffic.handler

import java.util.concurrent.LinkedBlockingQueue

import scalaz.Monad
import scalaz.Scalaz._
import eu.timepit.refined._
import messages._
import responseColors.ResponseColor
import scalaz.MonadError
import traffic.protocols.Protocol
import utils.logging.Logging

final class TrafficHandler[F[_, _] : MonadError : Logging](protocol: Protocol[F],
                                                        val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                                                        val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)]) {
  private val CHECKIMEI_MESSAGE_FORMAT = "([0-9]+)".r
  private val CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT = "([0-9]+);([0-9]+)".r

  def handleIncomingMessages(): F[String, Unit] =
    for {
      (address, checkImeiString) <- protocol.receiveMessage()
      checkImeiMessage <- mapToCorrectCheckImei(checkImeiString)
      _ <- MonadError[F].pure(checkImeiRequestQueue.put((address, checkImeiMessage)))
    } yield ()

  private def mapToCorrectCheckImei(checkImeiString: String) = {
    checkImeiString match {

      case CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT(imeiStr, imsiStr) =>
        val eImei: Either[String, Imei] = refineV(imeiStr)
        val eImsi: Either[String, Imsi] = refineV(imsiStr)
        for {
          imei <- eImei
          imsi <- eImsi
        } yield CheckImeiWithImsi(imei, imsi)

      case CHECKIMEI_MESSAGE_FORMAT(imeiStr) =>
        val eImei: Either[String, Imei] = refineV(imeiStr)
        for {
          imei <- eImei
        } yield CheckImei(imei)
    }
  }

  def handleOutgoingMessages(): F[Unit] =
    for {
      (address, color) <- MonadError[F].pure(checkImeiResponseQueue.take())
      _ <- Monad[F].pure(protocol.sendMessage(address, color))
    } yield ()
}
