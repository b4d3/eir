package faultManagement

sealed abstract class SeverityLevel

/**
  * Debug-level alarm
  */
case object DEBUG extends SeverityLevel

/**
  * Informational alarm
  */
case object INFO extends SeverityLevel

/**
  * Normal, but significant condition
  */
case object NOTICE extends SeverityLevel

/**
  * Warning condition
  */
case object WARNING extends SeverityLevel

/**
  * Error condition
  */
case object ERROR extends SeverityLevel

/**
  * Critical condition
  */
case object CRITICAL extends SeverityLevel

/**
  * Action must be taken immediately
  */
case object ALERT extends SeverityLevel

/**
  * System is unusable
  */
case object EMERGENCY extends SeverityLevel