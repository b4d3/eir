package traffic.protocols

trait Protocol[F[_]] {

  def send(message: String): F[String]
}
