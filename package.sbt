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

// filter out jar files from the list of generated files
mappings in Universal := (mappings in Universal).value.
  filter {
    case (_, name) =>  ! name.endsWith(".jar")
  }

// append the proguard jar file
mappings in Universal ++= (proguard in Proguard).value.map(jar => jar -> ("lib/" + jar.getName))

// point the classpath to the output from the proguard task
scriptClasspath := (proguard in Proguard).value.map(jar => jar.getName)

bashScriptExtraDefines ++= IO.readLines(sourceDirectory.value / "universal" / "conf" / "find_ip.sh")