val latestSha = settingKey[String]("latest-sha")

latestSha in ThisBuild := Process("git rev-parse HEAD").lines.head.take(6)

lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, latestSha, scalaVersion, sbtVersion),
    buildInfoPackage := "bad.robot.temperature"
  )