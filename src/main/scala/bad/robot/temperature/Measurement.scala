package bad.robot.temperature

import bad.robot.temperature.ds18b20.SensorReader
import bad.robot.temperature.rrd.Rrd

object Measurements {

  val filename = "src/test/resources/examples/28-000005e2fdc2/w1_slave"

  def randomTemperatureToRrd() = {
    new Measurement(RandomTemperatures(), Rrd())
  }

  def sensorToRrd() = {
    new Measurement(SensorReader(filename), Rrd())
  }

  def sensorToConsole() = {
    new Measurement(SensorReader(filename), Console())
  }
}

class Measurement(input: TemperatureReader, output: TemperatureWriter) extends Runnable {
  def run(): Unit = {
    input.read.fold(println, output.write)
  }
}
