package repository

import java.util.concurrent.{Executors, LinkedBlockingQueue}

import messages.CheckImeiMessage
import responseColors.ResponseColor

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

abstract class EirRepositoryHandler(val checkImeiRequestQueue: LinkedBlockingQueue[(String, CheckImeiMessage)],
                                    val checkImeiResponseQueue: LinkedBlockingQueue[(String, ResponseColor.Value)])
  extends EirRepository {

  private implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(
    Executors.newCachedThreadPool())

  new Thread(() => {

    while (true) {

      val (address, checkImeiMessage) = checkImeiRequestQueue.take()

      Future {

        val sendAddress = address
        val responseColorString = getResponseColor(checkImeiMessage)

        val responseColor = responseColorString match {
          case "WHITE" => ResponseColor.WHITE
          case "BLACK" => ResponseColor.BLACK
        }

        (sendAddress, responseColor)

      } onComplete {

        case Success(response) => checkImeiResponseQueue.put(response)
        case Failure(t) => println("Problem in querying repository:\n" + t)
      }
    }
  }).start()

}
