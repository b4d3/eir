package repository.alarms

import faultManagement.{Alarm, ERROR, SeverityLevel}

object RepositoryAlarms {

  object RepositoryUnreachable extends Alarm {

    override def alarmCode: Int = 1

    override def alarmMessage: String = "Repository unreachable"

    override def alarmSeverity: SeverityLevel = ERROR
  }

}
