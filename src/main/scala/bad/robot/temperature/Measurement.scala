package bad.robot.temperature

import java.io.PrintStream

case class Measurement(input: TemperatureReader, output: TemperatureWriter, error: PrintStream = System.err) extends Runnable {
  def run(): Unit = {
    input.read.fold(error.println, output.write)
  }
}
