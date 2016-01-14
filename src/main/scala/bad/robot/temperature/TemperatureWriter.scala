package bad.robot.temperature

trait TemperatureWriter {
  def write(id: SensorId, temperature: Temperature): Unit
}
