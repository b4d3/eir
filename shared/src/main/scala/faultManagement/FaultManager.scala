package faultManagement

import cats.effect.Sync
import cats.implicits._
import com.typesafe.scalalogging.Logger
import utils.logging.Logging

final class FaultManager[F[_] : Sync : Logging : AlarmThrottling](notifier: Notifier[F]) {

  implicit val logger: Logger = Logger(classOf[FaultManager[F]])

  def raiseAlarm(alarm: Alarm): F[Unit] =
    for {
      throttled <- AlarmThrottling[F].isThrottled(alarm)
      _ <- if (throttled) Logging[F].debug(s"Alarm $alarm throttled.")
      else notifier.notifyAlarmRaise(alarm) >> AlarmThrottling[F].updateThrottlingFor(alarm)
    } yield ()

  def clearAlarm(alarm: Alarm): F[Unit] =
    notifier.notifyAlarmClear(alarm) >> AlarmThrottling[F].clearThrottlingFor(alarm)
}