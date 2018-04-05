
## sbt-native-packager

[https://www.scala-sbt.org/sbt-native-packager](https://www.scala-sbt.org/sbt-native-packager)

Native packager only takes care of packaging, the act of putting a list of mappings (source file to install target path) into a distinct package format (zip, rpm, etc.).

Archetypes like Java Application Archetype or Java Server Application Archetype only add additional files to the mappings enriching the created package, but they don’t provide any new features for native-packager core functionality.

### Not in Scope

* Providing application lifecyle management.
    * The Java Server Application Archetype provides configurations for common systeloaders like SystemV, Upstart or SystemD. However create a custome solution, which includes stop scripts, PID management, etc. are not part of native packager.
* Providing deployment configurations
    * Native packager produces artefacts with the packageBin task. What you do with these is part of another step in your process.

### Core Concepts

1. [Packaging format plugins](https://www.scala-sbt.org/sbt-native-packager/introduction.html#format-plugins) - the _how_ an application is packaged; universal, linux, debian, rpm, docker, windows etc 
1. [Archetype plugins](https://www.scala-sbt.org/sbt-native-packager/introduction.html#archetype-plugins) - the _what_ gets packaged (incs.  predefined configurations); java application, java server application, system loaders etc
1. [Mappings](https://www.scala-sbt.org/sbt-native-packager/introduction.html#mappings) - map source files to target system locations

## Folder Structures

Each packaging format will expect files to include in your package to be in a specific folder. For example, the Universal plugin will look in `src\universal` by default.


## Packaging

### Java

Running `sbt universal:packageBin` would copy anything in `src\universal` into the generated zip file but without adding the Java format plugin, no class files would be included (so the zip would basically be empty).

Include `enablePlugins(JavaApplicationPlugin)` and it will copy in all the JARs (including the application). It also creates default startup scripts which, for us, are no good. They're close; they create an executable (and batch file) for each `App` and a general one to take care of the classpath etc.

So you'll get something like the following.

    total 80
    drwxr-xr-x  9 toby  staff    288  5 Apr 11:15 ./
    drwxr-xr-x  4 toby  staff    128  5 Apr 11:15 ../
    -rwxr-xr-x  1 toby  staff   1388  5 Apr 11:15 client*
    -rw-r--r--  1 toby  staff    110  5 Apr 11:15 client.bat
    -rwxr-xr-x  1 toby  staff   1388  5 Apr 11:15 server*
    -rw-r--r--  1 toby  staff    110  5 Apr 11:15 server.bat
    -rwxr-xr-x  1 toby  staff  11484  5 Apr 11:15 temperature-machine*
    -rw-r--r--  1 toby  staff   7530  5 Apr 11:15 temperature-machine.bat
    
Where `client` for example, ends up running the following.

    # execute the main start script
    $SCRIPTPATH/temperature-machine -main bad.robot.temperature.client.Client "$@"

...and `temperature-machine` handles classpath and Java setup etc.

Including 'enablePlugins(JavaServerAppPackaging)' adds some additional stuff.

### Debian

I think the control file will be defaulted if you don't override it (override by adding into `src\debian\DEBIAN`).

Package with `sbt debian:packageBin`.

When the `.deb` package is built, the same scripts are created in `temperature-machine_2.1-SNAPSHOT_all.deb/data/usr/bin`.

The "log" folder seems to default to `/var/lig/temperature-machine` although I don't know what will go in there.