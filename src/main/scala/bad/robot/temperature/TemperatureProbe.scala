package bad.robot.temperature

import bad.robot.temperature.rrd.Rrd

object TemperatureProbe {

  val filename = "src/test/resources/examples/28-000005e2fdc2/w1_slave"

  def rrdProbe() = {
    new TemperatureProbe(TemperatureReader(filename), Rrd())
  }

  def consoleProbe() = {
    new TemperatureProbe(TemperatureReader(filename), Console())
  }
}

class TemperatureProbe(input: TemperatureReader, output: TemperatureWriter) extends Runnable {
  def run(): Unit = {
    input.read.foreach(output.write)
  }
}
