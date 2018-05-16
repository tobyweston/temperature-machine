package bad.robot.logging

import java.io.File

import bad.robot.temperature.{FileOps, Files}

object LogFile {
  
  val file: File = Files.path / "temperature-machine.log"

}
