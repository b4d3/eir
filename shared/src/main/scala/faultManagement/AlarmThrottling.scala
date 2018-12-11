package faultManagement

import config.FmConfig
import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import scalaz.zio.{IO, Ref, system}
import simulacrum.typeclass

@typeclass
trait AlarmThrottling[F[_]] {

  def isThrottled(alarm: Alarm): F[Boolean]

  def updateThrottlingFor(alarm: Alarm): F[Unit]

  def clearThrottlingFor(alarm: Alarm): F[Unit]
}


object AlarmThrottling {

  implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  private val config = pureconfig.loadConfigOrThrow[FmConfig]("fm")


  def create(): IO[Nothing, AlarmThrottling[IO[Nothing, ?]]] =
    Ref[Map[Alarm, Vector[Long]]](Map()).map { m =>
      new AlarmThrottling[IO[Nothing, ?]] {

        private val throttlingPeriod = config.throttlingPeriod
        private val maxActiveAlarms = config.maxActiveAlarms

        private def removeAlarmsOlderThanThrottlingPeriod(): IO[Nothing, Unit] =
          for {
            currentTime <- system.currentTimeMillis
            _ <- m.update {
              _.mapValues(_.filter(i => (currentTime - i) >= throttlingPeriod))
            }
          } yield ()

        override def isThrottled(alarm: Alarm): IO[Nothing, Boolean] =
          for {
            _ <- removeAlarmsOlderThanThrottlingPeriod()
            map <- m.get
            throttled <- map.get(alarm) match {
              case Some(alarmTimestamps) => IO.point(alarmTimestamps.size < maxActiveAlarms)

              case None => IO.point(false)
            }
          } yield throttled

        override def updateThrottlingFor(alarm: Alarm): IO[Nothing, Unit] =
          for {
            currentTime <- system.currentTimeMillis
            map <- m.get
            containsAlarmTimestamps <- IO.point(map.contains(alarm))
            _ <- m.update { map =>
              if (containsAlarmTimestamps)
                map.updated(alarm, map(alarm) :+ currentTime)
              else
                map.updated(alarm, Vector(currentTime))
            }
          } yield ()

        override def clearThrottlingFor(alarm: Alarm): IO[Nothing, Unit] =
          for {
            _ <- m.update(_.updated(alarm, Vector()))
          } yield ()
      }
    }
}
