package bad.robot.temperature

import scalaz.\/

trait TemperatureWriter {
  def write(temperature: List[Temperature]): Error \/ Unit
}
