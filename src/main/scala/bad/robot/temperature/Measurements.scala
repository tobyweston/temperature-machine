package bad.robot.temperature

import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.Rrd

object Measurements {

  private val sensor = SensorFile.find().head

  def sensorToRrd() = {
    Measurement(SensorReader(sensor), Rrd())
  }

  def sensorToConsole() = {
    Measurement(SensorReader(sensor), Console())
  }
}
