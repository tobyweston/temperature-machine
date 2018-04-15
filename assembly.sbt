
assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

// fixes https://github.com/tobyweston/temperature-machine/issues/39
assemblyMergeStrategy in assembly := {
  case PathList(xs @ _*) if xs.last == "module-info.class" => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Remove ScalaDoc generation
sources in(Compile, doc) := Seq.empty
publishArtifact in(Compile, packageDoc) := false

addArtifact(artifact in(Compile, assembly), assembly)
