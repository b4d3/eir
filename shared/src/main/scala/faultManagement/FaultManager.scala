package faultManagement

import java.util.concurrent.{ConcurrentHashMap, LinkedBlockingQueue}

import pureconfig.generic.auto._
import com.typesafe.scalalogging.Logger
import config.FmConfig

trait FaultManager {

  private val logger = Logger(classOf[FaultManager])

  private val config = pureconfig.loadConfigOrThrow[FmConfig]("fm")
  private val throttlingPeriod = config.throttlingPeriod
  private val maxActiveAlarms = config.maxActiveAlarms

  private val throttledAlarms = new ConcurrentHashMap[Alarm, LinkedBlockingQueue[Long]]()

  private def removeOlderThanThrottlingPeriod(alarmTimesQueue: LinkedBlockingQueue[Long]) =

    Option(alarmTimesQueue.peek()) match {

      case Some(oldestAlarmTime) if (System.currentTimeMillis() - oldestAlarmTime) >=
        throttlingPeriod => alarmTimesQueue.poll()

      case _ => logger.trace(s"Alarms not older than throttlingPeriod ($throttlingPeriod ms)")
    }


  private def isThrottled(alarm: Alarm) = Option(throttledAlarms.get(alarm)) match {

    case Some(alarmTimesQueue) =>
      removeOlderThanThrottlingPeriod(alarmTimesQueue)
      if (alarmTimesQueue.size() < maxActiveAlarms) {
        alarmTimesQueue.put(System.currentTimeMillis())
        false
      } else true

    case None =>
      val alarmTimesQueue = new LinkedBlockingQueue[Long]()
      alarmTimesQueue.put(System.currentTimeMillis())
      throttledAlarms.put(alarm, alarmTimesQueue)
      false
  }

  /**
    * Override this for specific alarm raising notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be raised
    */
  protected def notifyAlarmRaise(alarm: Alarm): Unit

  /**
    * Override this for specific alarm clearing notification (e.g. over SNMP)
    *
    * @param alarm Alarm to be cleared
    */
  protected def notifyAlarmClear(alarm: Alarm): Unit

  def raiseAlarm(alarm: Alarm): Unit =
    if (!isThrottled(alarm))
      notifyAlarmRaise(alarm)
    else logger.debug(s"Alarm $alarm throttled.")

  def clearAlarm(alarm: Alarm): Unit = notifyAlarmClear(alarm)
}
