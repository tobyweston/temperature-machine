package bad.robot.temperature.rrd

import bad.robot.temperature.{Measurement, TemperatureWriter}

case class Rrd(monitored: List[Host]) extends TemperatureWriter {
  def write(measurement: Measurement) = RrdUpdate(monitored).apply(measurement)
}
