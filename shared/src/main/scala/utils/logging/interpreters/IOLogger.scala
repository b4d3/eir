package utils.logging.interpreters

import com.typesafe.scalalogging.Logger
import scalaz.zio.IO
import utils.logging.Logging

object IOLogger {

  val ioLogger = new Logging[IO[Nothing, ?]] {

    override def debug(message: String)(implicit logger: Logger): IO[Nothing, Unit] =
      IO.sync(logger.debug(message))

    override def info(message: String)(implicit logger: Logger): IO[Nothing, Unit] =
      IO.sync(logger.info(message))

    override def warn(message: String)(implicit logger: Logger): IO[Nothing, Unit] =
      IO.sync(logger.warn(message))

    override def error(message: String)(implicit logger: Logger): IO[Nothing, Unit] =
      IO.sync(logger.error(message))
  }
}
