package faultManagement

trait FaultManager {

  def raiseAlarm(alarm: Alarm)

  def clearAlarm(alarm: Alarm)
}
