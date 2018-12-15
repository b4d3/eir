package utils.logging

import com.typesafe.scalalogging.Logger
import simulacrum.typeclass

@typeclass trait Logging[F[_]] {

  def debug(message: String)(implicit logger: Logger): F[Unit]

  def info(message: String)(implicit logger: Logger): F[Unit]

  def warn(message: String)(implicit logger: Logger): F[Unit]

  def error(message: String)(implicit logger: Logger): F[Unit]
}
