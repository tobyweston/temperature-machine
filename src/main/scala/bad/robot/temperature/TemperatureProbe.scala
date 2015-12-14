package bad.robot.temperature

import java.text.SimpleDateFormat
import java.util.Calendar


object TemperatureProbe {
  def apply() = new TemperatureProbe("/Users/toby/Workspace/bitbucket/temperature-machine/src/test/resources/examples/28-000005e2fdc2/w1_slave")
}

class TemperatureProbe(filename: String) extends Runnable {

  def run(): Unit = {
    val temperature = TemperatureReader(filename).read
    temperature.foreach(temperature => println(s"$currentTime ${temperature.celsius} °C"))
  }

  def currentTime = {
    val today = Calendar.getInstance().getTime()
    val format = new SimpleDateFormat("dd/MM/YYYY HH:mm:ss")
    format.format(today)
  }
}
