package traffic.handler

import java.util.concurrent.LinkedBlockingQueue

import eu.timepit.refined._
import messages._
import responseColors.ResponseColor
import traffic.protocols.Protocol

abstract class TrafficHandler(val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                              val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor)])
  extends Protocol {

  private val CHECKIMEI_MESSAGE_FORMAT = "([0-9]+)".r
  private val CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT = "([0-9]+);([0-9]+)".r

  def startHandlingTraffic(): Unit = {

    new Thread(() => {

      while (true) {

        val (address, checkImeiString) = receiveMessage()

        val checkImeiMessage = checkImeiString match {

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

        checkImeiMessage.foreach { c =>
          checkImeiRequestQueue.put((address, c))
        }
      }
    }).start()

    new Thread(() => {

      while (true) {

        val (address, color) = checkImeiResponseQueue.take()
        sendMessage(address, color)
      }
    }).start()
  }

}
