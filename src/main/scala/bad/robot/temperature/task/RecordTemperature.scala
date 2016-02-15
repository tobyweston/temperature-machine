package bad.robot.temperature.task

import java.io.PrintStream

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.now
import bad.robot.temperature.{Measurement, TemperatureReader, TemperatureWriter}

case class RecordTemperature(host: Host, input: TemperatureReader, output: TemperatureWriter, error: PrintStream = System.err) extends Runnable {
  def run(): Unit = {
    input.read.fold(error.println, temperatures => {
      output.write(Measurement(host, now(), temperatures)).leftMap(error.println)
    })
  }
}
