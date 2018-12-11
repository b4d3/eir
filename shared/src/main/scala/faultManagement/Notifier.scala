package faultManagement

trait Notifier [F[_]] {

  def notifyAlarmRaise(alarm: Alarm): F[Unit]
  def notifyAlarmClear(alarm: Alarm): F[Unit]
}
