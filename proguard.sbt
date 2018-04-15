enablePlugins(SbtProguard)

proguardOptions in Proguard ++= Seq(
  "-dontnote",
  "-dontwarn",
  "-ignorewarnings",
  "-dontobfuscate",
  "-dontoptimize",
  "-printusage unused-code.txt",
  "-printconfiguration proguard.conf",
  "-keep class bad.robot.** { *; }",
  "-keep class scala.Symbol { *; }",
  "-keep enum ** { *; }",
  "-keepclassmembers class * { ** MODULE$; }"
)

proguardOptions in Proguard += ProguardOptions.keepMain("bad.robot.temperature.Main")

proguardInputs in Proguard := (dependencyClasspath in Compile).value.files

proguardFilteredInputs in Proguard ++= ProguardOptions.noFilter((packageBin in Compile).value)

javaOptions in(Proguard, proguard) := Seq("-Xmx2G")       // avoids out of memory (https://github.com/sbt/sbt-proguard/issues/3)

proguardInputFilter in Proguard := { file =>
  file.name match {
    case "log4j-api-2.11.0.jar"          => Some("!META-INF/**")            // https://sourceforge.net/p/proguard/bugs/665/
    case jar if jar.contains(name.value) => None                            // leave temperature-machine alone
    case _                               => Some("!META-INF/MANIFEST.MF")   // avoid proguard merge conflicts
  }
}  

//
//ProguardKeys.proguardVersion in Proguard := "5.2.1"
//
//ProguardKeys.options in Proguard ++= Seq(
//  "-dontwarn scala.**",
//  "-dontwarn com.google.code.tempusfugit.**",
//  "-dontwarn org.apache.**",
//  "-dontwarn scodec.bits.**",
//  "-dontwarn scalaparsers.**",

//  """
//    -keep public class bad.robot.** {
//      *;
//    }
//
//    -keep public class org.apache.commons.** {
//      *;
//     }
//
//    -keepclassmembers class * {
//      ** MODULE$;
//    }
//
//  """
//)
//
//
//
