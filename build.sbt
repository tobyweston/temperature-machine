
name := "temperature-machine"

version := "2.1"

organization := "bad.robot"

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

scalaVersion := "2.12.3"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(
  "org.rrd4j" % "rrd4j" % "2.2.1",
  "org.scalaz" %% "scalaz-core" % "7.2.15",
  "org.http4s" %% "http4s-dsl" % "0.16.3a",
  "org.http4s" %% "http4s-argonaut" % "0.16.3a",
  "org.http4s" %% "http4s-blaze-server" % "0.16.3a",
  "org.http4s" %% "http4s-blaze-client" % "0.16.3a",
  "io.argonaut" %% "argonaut" % "6.2",
  "org.slf4j" % "slf4j-simple" % "1.7.25",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "org.specs2" %% "specs2-core" % "3.9.5" % "test"
)

scalacOptions := Seq("-Xlint", "-Xfatal-warnings", "-deprecation", "-feature", "-language:implicitConversions,reflectiveCalls,higherKinds")
