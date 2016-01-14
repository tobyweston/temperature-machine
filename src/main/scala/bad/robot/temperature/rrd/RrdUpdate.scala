package bad.robot.temperature.rrd

import bad.robot.temperature.{SensorId, Temperature}
import org.rrd4j.core.RrdDb

case class RrdUpdate(id: SensorId, time: Seconds, temperature: Temperature) {
  def apply() = {
    val database = new RrdDb(RrdFile.file)
    val sample = database.createSample()
    sample.setTime(time)
    sample.setValue(s"sensor-${id.ordinal}", temperature.celsius)
    sample.update()
    database.close()
  }
}
