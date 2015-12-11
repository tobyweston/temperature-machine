
name := "temperature-machine"

scalaVersion := "2.11.7"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(
  "org.scalaz" %% "scalaz-core" % "7.1.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4",
  "org.specs2" %% "specs2-core" % "3.6.6" % "test"
)

resolvers ++= Seq(
  "bad.robot.repo" at "http://robotooling.com/maven"
)

scalacOptions := Seq("-Xlint", "-Xfatal-warnings", "-deprecation", "-feature", "-language:implicitConversions,reflectiveCalls,higherKinds")
