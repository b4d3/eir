package faultManagement

sealed trait SeverityLevel {

  def level: Int
}

/**
  * Debug-level alarm
  */
case object DEBUG extends SeverityLevel {
  val level = 0
}

/**
  * Informational alarm
  */
case object INFO extends SeverityLevel {
  val level = 1
}

/**
  * Normal, but significant condition
  */
case object NOTICE extends SeverityLevel {
  val level = 2
}

/**
  * Warning condition
  */
case object WARNING extends SeverityLevel {
  val level = 3
}

/**
  * Error condition
  */
case object ERROR extends SeverityLevel {
  val level = 4
}

/**
  * Critical condition
  */
case object CRITICAL extends SeverityLevel {
  val level = 5
}

/**
  * Action must be taken immediately
  */
case object ALERT extends SeverityLevel {
  val level = 6
}

/**
  * System is unusable
  */
case object EMERGENCY extends SeverityLevel {
  val level = 7
}
