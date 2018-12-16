package faultManagement

import com.typesafe.scalalogging.Logger
import utils.logging.Logging

final class LoggingNotifier[F[_] : Logging] extends Notifier[F] {

  private implicit val logger: Logger = Logger(classOf[LoggingNotifier[F]])

  /**
    * Override this for specific alarm raising notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be raised
    */
  override def notifyAlarmRaise(alarm: Alarm): F[Unit] = alarm.alarmSeverity match {

    case DEBUG => Logging[F].debug(s"$alarm")
    case INFO | NOTICE => Logging[F].info(s"$alarm")
    case WARNING => Logging[F].warn(s"$alarm")
    case ERROR | CRITICAL | ALERT | EMERGENCY => Logging[F].error(s"$alarm")
  }

  /**
    * Override this for specific alarm clearing notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be cleared
    */
  override def notifyAlarmClear(alarm: Alarm): F[Unit] = alarm.alarmSeverity match {

    case DEBUG => Logging[F].debug(s"CLEARED: $alarm")
    case _ => Logging[F].info(s"CLEARED: $alarm")
  }
}
