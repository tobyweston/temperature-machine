package bad.robot.temperature.ds18b20

import java.io.{File, FileFilter}

import bad.robot.temperature.SensorId
import bad.robot.temperature.rrd._

object SensorFile {

  val BaseFolder = "/sys/bus/w1/devices/"

  val SensorFiles: (File) => Boolean = file => file.isDirectory && file.getName.startsWith("28-")

  implicit def functionToFileFilter(function: (File) => Boolean): FileFilter = new FileFilter {
    def accept(file: File): Boolean = function(file)
  }

  def find(base: String = BaseFolder): List[SensorFile] = {
    val files = Option(new File(base).listFiles(SensorFiles))

    files.map(array => array.map(folder => {
      SensorFile(SensorId(folder.getName), folder / "w1_slave")
    }).toList).getOrElse(List())
  }
}

case class SensorFile(id: SensorId, file: File)