package bad.robot.temperature

import scalaz.\/

trait TemperatureWriter {
  def write(measurement: Measurement): Error \/ Unit
}
