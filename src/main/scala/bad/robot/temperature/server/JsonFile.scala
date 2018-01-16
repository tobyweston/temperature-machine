package bad.robot.temperature.server

import java.io.File

import bad.robot.temperature.{Error, FileError, FileOps}

import scala.io.Source
import scalaz.\/
import scalaz.\/.fromTryCatchNonFatal

object JsonFile {

  val path = new File(sys.props("user.home")) / ".temperature"
  val filename = "temperature.json"
  val file = path / filename
  
  path.mkdirs()


  def load: Error \/ String = {
    fromTryCatchNonFatal(Source.fromFile(file).getLines().mkString("\n")).leftMap(FileError)
  }

}
