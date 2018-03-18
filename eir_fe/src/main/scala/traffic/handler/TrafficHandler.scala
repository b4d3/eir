package traffic.handler

import java.util.concurrent.LinkedBlockingQueue

import messages._
import responseColors.ResponseColor
import traffic.protocols.Protocol

abstract class TrafficHandler(val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                              val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor.Value)])
  extends Protocol {

  //TODO Correct number of digits per IMEI and IMSI
  private val CHECKIMEI_MESSAGE_FORMAT = "([0-9]+)".r
  private val CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT = "([0-9]+);([0-9]+)".r

  def startHandlingTraffic(): Unit = {

    new Thread(() => {

      while (true) {

        val (address, checkImeiString) = receiveMessage()

        val checkImeiMessage = checkImeiString match {

          case CHECKIMEI_WITH_IMSI_MESSAGE_FORMAT(imei, imsi) => CheckImeiWithImsi(Imei
          (imei), Imsi(imsi))
          case CHECKIMEI_MESSAGE_FORMAT(imei) => CheckImei(Imei(imei))
        }

        checkImeiRequestQueue.put((address, checkImeiMessage))
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
