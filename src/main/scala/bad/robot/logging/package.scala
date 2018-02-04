package bad.robot

import bad.robot.temperature.rrd.{RrdFile, _}
import bad.robot.temperature.FileOps
import org.apache.logging.log4j.LogManager

package object logging {

  sys.props += ("log.location" -> RrdFile.path / "temperature-machine.log")

  val Log = LogManager.getLogger("bad.robot.temperature")

}
