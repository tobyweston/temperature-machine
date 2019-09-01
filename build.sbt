import Versions._

name := "temperature-machine"

version := "2.2.1"

organization := "bad.robot"

scalaVersion := "2.12.4"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(
  "org.rrd4j" % "rrd4j" % "2.2.1",
  "org.scalaz" %% "scalaz-core" % "7.2.17",
  "org.http4s" %% "http4s-dsl" % http4s,
  "org.http4s" %% "http4s-circe" % http4s,
  "org.http4s" %% "http4s-blaze-server" % http4s,
  "org.http4s" %% "http4s-blaze-client" % http4s,
  "io.circe" %% "circe-core" % circe,
  "io.circe" %% "circe-generic" % circe,
  "io.circe" %% "circe-parser" % circe,
  "io.circe" %% "circe-literal" % circe,
  "org.apache.logging.log4j" % "log4j-api" % log4j,
  "org.apache.logging.log4j" % "log4j-core" % log4j,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4j,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.0",
  "io.verizon.knobs" %% "core" % "6.0.33",
  "org.specs2" %% "specs2-core" % "3.9.5" % "test"
)

scalacOptions := Seq("-Xlint", "-Xfatal-warnings", "-deprecation", "-feature", "-language:implicitConversions,reflectiveCalls,higherKinds", "-Ypartial-unification")

