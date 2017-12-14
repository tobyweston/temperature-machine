package bad.robot

import bad.robot.temperature.rrd.{RrdFile, _}
import org.apache.logging.log4j.LogManager

package object logging {

  sys.props +=
    ("org.slf4j.simpleLogger.logFile"                   ->  RrdFile.path / "temperature-machine.log") +=
    ("org.slf4j.simpleLogger.defaultLogLevel"           -> "error") +=
    ("org.slf4j.simpleLogger.log.bad.robot.temperature" -> "info") +=
    ("org.slf4j.simpleLogger.showDateTime"              -> "true") +=
    ("org.slf4j.simpleLogger.dateTimeFormat"            -> "\u0000yyyy-MM-dd HH:mm:ss:SSS") +=
    ("org.slf4j.simpleLogger.showThreadName"            -> "true") +=
    ("org.slf4j.simpleLogger.showLogName"               -> "false") +=
    ("org.slf4j.simpleLogger.showShortLogName"          -> "false")

  sys.props +=
    ("log.location"                                     -> RrdFile.path / "temperature-machine.log")
  
  val Log = LogManager.getLogger("bad.robot.temperature")

}
