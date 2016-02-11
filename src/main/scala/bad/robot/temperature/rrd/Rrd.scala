package bad.robot.temperature.rrd

import bad.robot.temperature.{Measurement, TemperatureWriter}

case class Rrd() extends TemperatureWriter {
  def write(measurement: Measurement) = RrdUpdate(measurement).apply()
}
