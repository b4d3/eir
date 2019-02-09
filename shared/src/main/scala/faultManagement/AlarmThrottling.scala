package faultManagement

import cats.effect._
import cats.effect.concurrent.Ref
import config.FmConfig
import pureconfig.{CamelCase, ConfigFieldMapping}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import simulacrum.typeclass
import cats.implicits._

import scala.concurrent.duration.MILLISECONDS

@typeclass
trait AlarmThrottling[F[_]] {

  def isThrottled(alarm: Alarm): F[Boolean]

  def updateThrottlingFor(alarm: Alarm): F[Unit]

  def clearThrottlingFor(alarm: Alarm): F[Unit]
}


object AlarmThrottling {

  private implicit def hint[T]: ProductHint[T] = ProductHint[T](ConfigFieldMapping(CamelCase,
    CamelCase))

  private val config = pureconfig.loadConfigOrThrow[FmConfig]("fm")

  def create[F[_]: Sync]()(implicit clock: Clock[F]): F[AlarmThrottling[F]] =
    Ref.of[F, Map[Alarm, Vector[Long]]](Map()).map { m =>
      new AlarmThrottling[F] {

        private val throttlingPeriod = config.throttlingPeriod
        private val maxActiveAlarms = config.maxActiveAlarms

        private def removeAlarmsOlderThanThrottlingPeriod(): F[Unit] =
          for {
            currentTime <- clock.monotonic(MILLISECONDS)
            _ <- m.update {
              _.mapValues(_.filter(i => (currentTime - i) <= throttlingPeriod))
            }
          } yield ()

        override def isThrottled(alarm: Alarm): F[Boolean] =
          for {
            _ <- removeAlarmsOlderThanThrottlingPeriod()
            map <- m.get
            throttled <- map.get(alarm) match {
              case Some(alarmTimestamps) => Sync[F].delay(alarmTimestamps.size >= maxActiveAlarms)

              case None => Sync[F].pure(false)
            }
          } yield throttled

        override def updateThrottlingFor(alarm: Alarm): F[Unit] =
          for {
            currentTime <- clock.monotonic(MILLISECONDS)
            map <- m.get
            containsAlarmTimestamps <- Sync[F].delay(map.contains(alarm))
            _ <- m.update { map =>
              if (containsAlarmTimestamps)
                map.updated(alarm, map(alarm) :+ currentTime)
              else
                map.updated(alarm, Vector(currentTime))
            }
          } yield ()

        override def clearThrottlingFor(alarm: Alarm): F[Unit] =
          for {
            _ <- m.update(_.updated(alarm, Vector()))
          } yield ()
      }
    }
}
