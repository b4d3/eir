package faultManagement
import eu.timepit.refined.api.Refined
import eu.timepit.refined.collection.NonEmpty

final case class Alarm(alarmCode: Int, alarmMessage: String Refined NonEmpty,
                       alarmSeverity: SeverityLevel)
