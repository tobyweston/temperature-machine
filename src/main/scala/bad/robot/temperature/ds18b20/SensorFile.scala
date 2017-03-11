package bad.robot.temperature.ds18b20

import java.io.{File, FileFilter}

import bad.robot.temperature.FailedToFindFile
import bad.robot.temperature.rrd._

import scalaz.concurrent.Task

object SensorFile {

  val BaseFolder = "/sys/bus/w1/devices/"

  val SensorFiles: (File) => Boolean = file => file.isDirectory && file.getName.startsWith("28-")

  implicit def functionToFileFilter(function: (File) => Boolean): FileFilter = new FileFilter {
    def accept(file: File): Boolean = function(file)
  }

  def findSensorsAndExecute[A](task: List[SensorFile] => Task[A]) = {
    val location = sys.props.getOrElse("sensor.location", BaseFolder)

    SensorFile.find(location) match {
      case Nil     => println(FailedToFindFile(location).message)
      case sensors => task(sensors).unsafePerformSync
    }
  }

  def find(base: String = BaseFolder): List[SensorFile] = {
    val files = Option(new File(base).listFiles(SensorFiles))
    files.map(array => array.map(_ / "w1_slave").toList).getOrElse(List())
  }
}