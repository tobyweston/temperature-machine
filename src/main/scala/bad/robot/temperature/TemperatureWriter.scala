package bad.robot.temperature

trait TemperatureWriter {
  def write(temperature: Temperature): Unit
}
