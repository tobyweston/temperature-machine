package bad.robot.temperature

import bad.robot.temperature.task.FixedTimeMeasurement

import scalaz.\/

trait TemperatureWriter {
  def write(measurement: Measurement): Error \/ Unit
}

trait FixedTimeMeasurementWriter {
  def write(measurement: FixedTimeMeasurement): Error \/ List[SensorReading]
}
