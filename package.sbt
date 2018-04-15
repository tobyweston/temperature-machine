enablePlugins(JavaServerAppPackaging, DebianPlugin, JDebPackaging, SystemdPlugin)

maintainer := "Toby Weston <toby@temperature-machine.com>"

packageSummary := "temperature-machine"

packageDescription := """Homebrew temperature data logger based on the DS18B20 sensor."""

debianPackageDependencies in Debian ++= Seq("java8-runtime | oracle-java8-jdk")

//linuxPackageMappings in Debian += {
//  val file = sourceDirectory.value / "debian" / "bin" / "start-server.sh"
//  packageMapping( 
//    (file, "/usr/share/temperature-machine/bin/start-server.sh") 
//  ) withUser "pi" withGroup "pi" withPerms "644"
//}

linuxPackageMappings in Debian += packageDirectoryAndContentsMapping(
  (sourceDirectory.value / "debian" / "bin") -> s"/usr/share/${packageName.value}/bin"
)

linuxPackageMappings in Debian += packageDirectoryAndContentsMapping(
  (sourceDirectory.value / "debian" / "conf") -> s"/usr/share/${packageName.value}/conf"
) withUser "pi" withGroup "pi" withPerms "644"

// don't package JavaDoc or source
mappings in (Compile, packageDoc) := Seq()
mappings in (Compile, packageSrc) := Seq()

//linuxPackageMappings := {
//  val mappings = linuxPackageMappings.value
//  mappings map { mapping =>
//    val filtered = mapping.mappings filter {
//      case (file, name) if file.getParent == "man1" => name.endsWith(".txt")
//      case (file, name) if file.getParent == "man1" => name.endsWith(".md")
//      case _                                        => false
//    }
//    mapping.copy(mappings = filtered)
//  } filter {
//    _.mappings.nonEmpty
//  }
//}

// remove all jar mappings in universal and append the "fat" jar
mappings in Universal := {
  val universalMappings = (mappings in Universal).value
  val fatJar = (assembly in Compile).value
  val filtered = universalMappings filter {
    case (_, name) =>  ! name.endsWith(".jar")
  }
  filtered :+ (fatJar -> ("lib/" + fatJar.getName))
}

// the bash scripts classpath only needs the fat jar
scriptClasspath := Seq((assemblyJarName in assembly).value)