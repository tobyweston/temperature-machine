package bad.robot.temperature.rrd

import bad.robot.temperature.Temperature
import org.rrd4j.core.RrdDb

case class RrdUpdate(time: Seconds, temperatures: List[Temperature]) {
  def apply() = {
    val database = new RrdDb(RrdFile.file)
    val sample = database.createSample()
    sample.setTime(time)
    sample.setValues(temperatures.map(_.celsius):_*)
    sample.update()
    database.close()
  }
}
