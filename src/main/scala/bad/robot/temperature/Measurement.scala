package bad.robot.temperature

class Measurement(input: TemperatureReader, output: TemperatureWriter) extends Runnable {
  def run(): Unit = {
    input.read.fold(println, output.write)
  }
}
