package bad.robot.temperature.rrd

import bad.robot.temperature.{Temperature, TemperatureWriter}

case class Rrd() extends TemperatureWriter {
  def write(temperatures: List[Temperature]) = RrdUpdate(now(), temperatures).apply()
}
