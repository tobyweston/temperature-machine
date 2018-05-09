enablePlugins(JavaServerAppPackaging, DebianPlugin, JDebPackaging, SystemdPlugin)

maintainer := "Toby Weston <toby@temperature-machine.com>"

packageSummary := "temperature data logger based on the DS18B20 sensor"

packageDescription := """Homebrew temperature data logger based on the DS18B20 sensor."""

debianPackageDependencies in Debian ++= Seq("java8-runtime | oracle-java8-jdk")


// don't package JavaDoc or source
mappings in (Compile, packageDoc) := Seq()
mappings in (Compile, packageSrc) := Seq()

// todo add a mapping from /var/log/temperature-machine/xxx.log to symlink /home/pi/.temperature/temperature-machine.log

daemonUser in Linux := "pi"

linuxPackageMappings := {
  val mappings = linuxPackageMappings.value
  mappings map { mapping =>
    val filtered = mapping.mappings filter {
      case (file, _) => !(file.getName == "temperature-machine.1.md")
      case _         => true
    }
    mapping.copy(mappings = filtered)
  } filter {
    _.mappings.nonEmpty
  }
}

// filter out jar files from the list of generated files
mappings in Universal := (mappings in Universal).value.
  filter {
    case (_, name) => !name.endsWith(".jar")
  }

// append the proguard jar file
mappings in Universal ++= (proguard in Proguard).value.map(jar => jar -> ("lib/" + jar.getName))

// point the classpath to the output from the proguard task
scriptClasspath := (proguard in Proguard).value.map(jar => jar.getName)

// add some custom stuff to the startup scripts
bashScriptExtraDefines ++= IO.readLines(sourceDirectory.value / "universal" / "conf" / "find_ip.sh")
bashScriptExtraDefines += """addJava "-Djava.rmi.server.hostname=$(getIpAddress)""""