package bad.robot.temperature

import scalaz.\/

trait TemperatureReader {
  def read: Error \/ Measurement
}
