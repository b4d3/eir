package faultManagement

import com.typesafe.scalalogging.Logger

class LoggingFaultManager extends FaultManager {

  private val logger = Logger(classOf[LoggingFaultManager])

  /**
    * Override this for specific alarm raising notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be raised
    */
  override protected def notifyAlarmRaise(alarm: Alarm): Unit = alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"$alarm")
    case INFO | NOTICE => logger.info(s"$alarm")
    case WARNING => logger.warn(s"$alarm")
    case ERROR | CRITICAL | ALERT | EMERGENCY => logger.error(s"$alarm")
  }

  /**
    * Override this for specific alarm clearing notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be cleared
    */
  override protected def notifyAlarmClear(alarm: Alarm): Unit = alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"CLEARED: $alarm")
    case _ => logger.info(s"CLEARED: $alarm")
  }
}
