import scala.sys.process._

val latestSha = settingKey[String]("latest-sha")

latestSha in ThisBuild := "git rev-parse HEAD".lineStream_!.head.take(6)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, latestSha, scalaVersion, sbtVersion),
    buildInfoPackage := "bad.robot.temperature"
  )