package bad.robot.temperature.task

import java.io.PrintStream

import bad.robot.temperature.{TemperatureReader, TemperatureWriter}

case class RecordTemperature(input: TemperatureReader, output: TemperatureWriter, error: PrintStream = System.err) extends Runnable {
  def run(): Unit = {
    input.read.fold(error.println, output.write)
  }
}
