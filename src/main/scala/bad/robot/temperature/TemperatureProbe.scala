package bad.robot.temperature


object TemperatureProbe {
  def apply() = new TemperatureProbe("/Users/toby/Workspace/bitbucket/temperature-machine/src/test/resources/examples/28-000005e2fdc2/w1_slave")
}

class TemperatureProbe(filename: String) extends Runnable {

  def run(): Unit = {
    val temperature = TemperatureReader(filename).read
    temperature.foreach(temperature => println(temperature.celsius + " °C"))
  }
}
