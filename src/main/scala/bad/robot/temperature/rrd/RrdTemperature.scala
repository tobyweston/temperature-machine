package bad.robot.temperature.rrd

import bad.robot.temperature.Temperature
import org.rrd4j.core.RrdDb
import RrdTemperature._

object RrdTemperature {
  val name = "temperature-1"
}

case class RrdTemperature(time: Long, temperature: Temperature) {
  def apply() = {
    val database = new RrdDb(RrdFile.path)
    val sample = database.createSample()
    sample.setTime(time)
    sample.setValue(name, temperature.celsius)
    sample.update()
    database.close()
  }
}
