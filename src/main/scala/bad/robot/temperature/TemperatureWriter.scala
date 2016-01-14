package bad.robot.temperature

trait TemperatureWriter {
  def write(temperature: List[Temperature]): Unit
}
