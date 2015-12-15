package bad.robot.temperature.rrd

import bad.robot.temperature.{Temperature, TemperatureWriter}

case class Rrd() extends TemperatureWriter {
  def write(temperature: Temperature) = RrdUpdate(now(), temperature).apply()
}
