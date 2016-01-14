package bad.robot.temperature

case class Measurement(input: TemperatureReader, output: TemperatureWriter) extends Runnable {
  def run(): Unit = {
    input.read.fold(println, temperatures => temperatures.foreach {
      case (id, temperature) => output.write(id, temperature)
    })
  }
}
