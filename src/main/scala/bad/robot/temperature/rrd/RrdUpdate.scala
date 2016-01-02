package bad.robot.temperature.rrd

import bad.robot.temperature.Temperature
import org.rrd4j.core.RrdDb
import RrdUpdate._

object RrdUpdate {
  val name = "temperature-1"
}

case class RrdUpdate(time: Seconds, temperature: Temperature) {
  def apply() = {
    val database = new RrdDb(RrdFile.file)
    val sample = database.createSample()
    sample.setTime(time)
    sample.setValue(name, temperature.celsius)
    sample.update()
    database.close()
  }
}
