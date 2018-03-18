name := "eir"

lazy val commonSettings = Seq(
  organization := "com.bade",
  version := "0.1",
  scalaVersion := "2.12.4",
  libraryDependencies += "org.zeromq" % "jeromq" % "0.4.3"
)

lazy val root = (project in file(".")).settings(commonSettings).aggregate(shared, eir_fe, traffgen)

lazy val shared = project.settings(commonSettings)
lazy val eir_fe = project.settings(commonSettings).dependsOn(shared)
lazy val traffgen = project.settings(commonSettings).dependsOn(shared)
