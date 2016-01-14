package bad.robot.temperature.rrd

import bad.robot.temperature.{SensorId, Temperature, TemperatureWriter}

case class Rrd() extends TemperatureWriter {
  def write(id: SensorId, temperature: Temperature) = RrdUpdate(id, now(), temperature).apply()
}
