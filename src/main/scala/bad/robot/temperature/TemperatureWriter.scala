package bad.robot.temperature

import bad.robot.temperature.rrd.Host

import scalaz.\/

trait TemperatureWriter {
  def write(measurement: Measurement): Error \/ Unit
}
