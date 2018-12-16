package utils.logging.interpreters

import cats.effect.IO
import com.typesafe.scalalogging.Logger
import utils.logging.Logging

object IOLogger {

  val ioLogger = new Logging[IO] {

    override def debug(message: String)(implicit logger: Logger): IO[Unit] =
      IO(logger.debug(message))

    override def info(message: String)(implicit logger: Logger): IO[Unit] =
      IO(logger.info(message))

    override def warn(message: String)(implicit logger: Logger): IO[Unit] =
      IO(logger.warn(message))

    override def error(message: String)(implicit logger: Logger): IO[Unit] =
      IO(logger.error(message))
  }
}
