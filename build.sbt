
name := "temperature-machine"

version := "2.1"

organization := "bad.robot"

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

scalaVersion := "2.12.4"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(
  "org.rrd4j" % "rrd4j" % "2.2.1",
  "org.scalaz" %% "scalaz-core" % "7.2.17",
  "org.http4s" %% "http4s-dsl" % "0.16.6a",
  "org.http4s" %% "http4s-argonaut" % "0.16.6a",
  "org.http4s" %% "http4s-blaze-server" % "0.16.6a",
  "org.http4s" %% "http4s-blaze-client" % "0.16.6a",
  "io.argonaut" %% "argonaut" % "6.2",
  "org.apache.logging.log4j" % "log4j-api" % "2.10.0",
  "org.apache.logging.log4j" % "log4j-core" % "2.10.0",
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % "2.10.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
  "org.specs2" %% "specs2-core" % "3.9.5" % "test"
)

scalacOptions := Seq("-Xlint", "-Xfatal-warnings", "-deprecation", "-feature", "-language:implicitConversions,reflectiveCalls,higherKinds")

// fixes https://github.com/tobyweston/temperature-machine/issues/39
assemblyMergeStrategy in assembly := {
  case PathList(xs @ _*) if xs.last == "module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}