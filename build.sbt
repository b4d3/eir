import sbt.Keys.resolvers
import sbt.addCompilerPlugin

name := "eir"

lazy val commonSettings = Seq(
  organization := "com.bade",
  version := "0.2",
  scalaVersion := "2.12.7",
  libraryDependencies += "org.zeromq" % "jeromq" % "0.4.3",
  libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.8.0",
  libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3",
  libraryDependencies += "com.typesafe" % "config" % "1.3.2",
  libraryDependencies += "eu.timepit" %% "refined-scalaz" % "0.9.3",
  libraryDependencies += "org.scalaz" %% "scalaz-zio" % "0.5.0",
  libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.10.1",
  libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.2.27",
  libraryDependencies += "com.github.mpilquist" %% "simulacrum" % "0.14.0",


  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.8")
)

lazy val root = (project in file(".")).settings(commonSettings).aggregate(shared, eir_fe, traffgen)

lazy val shared = project.settings(commonSettings)

lazy val eir_fe = project
  .settings(commonSettings,
    libraryDependencies += "com.unboundid" % "unboundid-ldapsdk" % "4.0.5"
  ).dependsOn(shared)

lazy val traffgen = project.settings(commonSettings).dependsOn(shared)

