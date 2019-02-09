package traffic.handler

import java.util.concurrent.LinkedBlockingQueue

import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.Logger
import eu.timepit.refined._
import messages._
import responseColors.ResponseColor
import traffic.protocols.Protocol
import utils.logging.Logging

object TrafficHandler {

  def apply[F[_] : Sync : Logging](protocol: Protocol[F],
                                   checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                                   checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)])
  : F[TrafficHandler[F]] =
    Sync[F].delay(new TrafficHandler[F](protocol, checkImeiRequestQueue, checkImeiResponseQueue))
}


final class TrafficHandler[F[_] : Sync : Logging] private(protocol: Protocol[F],
                                                          val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                                                          val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)]) {

  private implicit val logger = Logger(classOf[TrafficHandler[F]])

  private val CHECKIMEI_MESSAGE_FORMAT = "([0-9]+)".r
  private val CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT = "([0-9]+);([0-9]+)".r

  def handleIncomingMessage(): F[Unit] =
    for {
      addressAndCheckImei <- protocol.receiveMessage()
      (address, checkImeiString) = addressAndCheckImei
      checkImeiMessage = mapToCorrectCheckImei(checkImeiString)
      _ <- checkImeiMessage match {
        case Right(cim) => Sync[F].delay(checkImeiRequestQueue.put((address, cim)))
        case Left(e) => Logging[F].error("Received wrongly formatted CheckImei: " + e)
      }
    } yield ()


  private def mapToCorrectCheckImei(checkImeiString: String) =
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


  def handleOutgoingMessage(): F[Unit] =
    for {
      (address, color) <- Sync[F].delay(checkImeiResponseQueue.take())
      _ <- protocol.sendMessage(address, color)
      _ <- Logging[F].info("SENT: " + color)
    } yield ()
}
