package utils

import simulacrum.typeclass

@typeclass trait Logging[F[_]] {

  def debug[T](message: String)(implicit C: Class[T]): F[Unit]
}
