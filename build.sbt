
name := "temperature-machine"

scalaVersion := "2.11.7"

mainClass in Compile := Some("bad.robot.temperature.Main")

libraryDependencies ++= Seq(

)

resolvers ++= Seq(
  "bad.robot.repo" at "http://robotooling.com/maven"
)
