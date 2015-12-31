package bad.robot.temperature

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.Rrd

object Measurements {

  private val sensor = SensorFile.find().head

  def randomTemperatureToRrd() = {
    new Measurement(RandomTemperatures(), Rrd())
  }

  def sensorToRrd() = {
    new Measurement(SensorReader(sensor), Rrd())
  }

  def sensorToConsole() = {
    new Measurement(SensorReader(sensor), Console())
  }
}
