
name := "temperature-machine"

version := "2.0"

organization := "bad.robot"

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

scalaVersion := "2.12.1"
sbtVersion := "0.13.13"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(
  "org.rrd4j" % "rrd4j" % "2.2.1",
  "org.scalaz" %% "scalaz-core" % "7.2.9",
  "org.http4s" %% "http4s-dsl" % "0.15.6a",
  "org.http4s" %% "http4s-argonaut" % "0.15.6a",
  "org.http4s" %% "http4s-blaze-server" % "0.15.6a",
  "org.http4s" %% "http4s-blaze-client" % "0.15.6a",
  "io.argonaut" %% "argonaut" % "6.2-RC2",
  "org.slf4j" % "slf4j-simple" % "1.7.18",
  "org.specs2" %% "specs2-core" % "3.8.9" % "test"
)

scalacOptions := Seq("-Xlint", "-Xfatal-warnings", "-deprecation", "-feature", "-language:implicitConversions,reflectiveCalls,higherKinds")
