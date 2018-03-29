package faultManagement

import com.typesafe.scalalogging.Logger

class LoggingFaultManager extends FaultManager {

  private val logger = Logger(classOf[LoggingFaultManager])

  override def raiseAlarm(alarm: Alarm): Unit = alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"$alarm")
    case INFO | NOTICE => logger.info(s"$alarm")
    case WARNING => logger.warn(s"$alarm")
    case ERROR | CRITICAL | ALERT | EMERGENCY => logger.error(s"$alarm")
  }

  override def clearAlarm(alarm: Alarm): Unit = alarm.alarmSeverity match {

    case DEBUG => logger.debug(s"CLEARED: $alarm")
    case _ => logger.info(s"CLEARED: $alarm")
  }

}
