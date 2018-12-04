package repository.alarms

import eu.timepit.refined.auto._
import faultManagement.{Alarm, ERROR}

object RepositoryAlarms {

  val REPOSITORY_UNREACHABLE = Alarm(1, "Repository unreachable", ERROR)
}
