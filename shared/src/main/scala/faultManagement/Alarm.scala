package faultManagement

trait Alarm {

  def alarmCode: Int

  def alarmMessage: String

  def alarmSeverity: SeverityLevel

}
