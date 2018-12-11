package faultManagement

import com.typesafe.scalalogging.Logger
import scalaz.Applicative
import scalaz.Scalaz._

class LoggingNotifier[F[_] : Applicative] extends Notifier[F] {

  private val logger = Logger(classOf[LoggingNotifier[F]])

  /**
    * Override this for specific alarm raising notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be raised
    */
  override def notifyAlarmRaise(alarm: Alarm): F[Unit] = (alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"$alarm")
    case INFO | NOTICE => logger.info(s"$alarm")
    case WARNING => logger.warn(s"$alarm")
    case ERROR | CRITICAL | ALERT | EMERGENCY => logger.error(s"$alarm")
  }).pure[F]

  /**
    * Override this for specific alarm clearing notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be cleared
    */
  override def notifyAlarmClear(alarm: Alarm): F[Unit] = (alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"CLEARED: $alarm")
    case _ => logger.info(s"CLEARED: $alarm")

  }).pure[F]
}
