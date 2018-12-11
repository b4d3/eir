package config

sealed case class FmConfig(maxActiveAlarms: Int, throttlingPeriod: Int)