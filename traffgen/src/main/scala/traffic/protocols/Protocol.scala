package traffic.protocols

trait Protocol[F[_]] {

  def sendAndReceiveResp(message: String): F[String]
}
