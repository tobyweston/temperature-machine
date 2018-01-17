package bad.robot.temperature.ds18b20

import java.io.{File, FileFilter}

import bad.robot.temperature.rrd._
import bad.robot.temperature.{Error, FailedToFindFile}

import scala.{Error => _}
import scalaz.\/
import cats.effect.IO
import scalaz.syntax.either.ToEitherOps

object SensorFile {

  val BaseFolder = "/sys/bus/w1/devices/"

  val SensorFiles: (File) => Boolean = file => file.isDirectory && file.getName.startsWith("28-")

  implicit def functionToFileFilter(function: (File) => Boolean): FileFilter = new FileFilter {
    def accept(file: File): Boolean = function(file)
  }

  def findSensorsAndExecute[A](task: List[SensorFile] => IO[A]): Error \/ A = {
    val location = sys.props.getOrElse("sensor.location", BaseFolder)

    SensorFile.find(location) match {
      case Nil     => FailedToFindFile(location).left
      case sensors => task(sensors).unsafeRunSync.right
    }
  }

  def find(base: String = BaseFolder): List[SensorFile] = {
    val files = Option(new File(base).listFiles(SensorFiles))
    files.map(array => array.map(_ / "w1_slave").toList).getOrElse(List())
  }
}