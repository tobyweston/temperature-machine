package bad.robot.temperature.server

import bad.robot.temperature.{Error, FileError, FileOps, Files}
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

import scala.io.Source

object JsonFile {

  val filename = "temperature.json"
  val file = Files.path / filename
  

  def exists = file.exists()

  def load: Error \/ String = {
    fromTryCatchNonFatal(Source.fromFile(file).getLines().mkString(sys.props("line.separator"))).leftMap(FileError)
  }

}
