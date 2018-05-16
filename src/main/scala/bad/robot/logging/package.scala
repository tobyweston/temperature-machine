package bad.robot

import bad.robot.temperature.Files._
import cats.effect.IO
import org.apache.logging.log4j.LogManager

package object logging {

  sys.props += ("log.location" -> LogFile.file)

  val Log = LogManager.getLogger("bad.robot.temperature")

  def info(message: String) = IO(Log.info(message))
  
  def error(message: String) = IO(Log.error(message))
}
