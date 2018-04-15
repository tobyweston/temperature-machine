addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.6")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")

addSbtPlugin("com.47deg" %% "sbt-microsites" % "0.7.14")

addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.7.0")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.0.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.3")

addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")

libraryDependencies += "org.vafer" % "jdeb" % "1.6" artifacts Artifact("jdeb", "jar", "jar") 