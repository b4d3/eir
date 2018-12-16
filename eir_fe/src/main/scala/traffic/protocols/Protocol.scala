package traffic.protocols

import responseColors.ResponseColor

trait Protocol[F[_]] {

  def receiveMessage(): F[(String, String)]

  def sendMessage(address: String, responseColor: ResponseColor): F[Unit]
}
