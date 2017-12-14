package bad.robot.temperature.task

import java.io.PrintStream

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.rrd.Seconds.now
import bad.robot.temperature.{Error, Measurement, TemperatureReader, TemperatureWriter}

case class RecordTemperature(host: Host, input: TemperatureReader, output: TemperatureWriter, error: PrintStream = System.err) extends Runnable {
  def onError(stream: PrintStream): Error => Unit = error => stream.print(error + "\u0000")  
  
  def run(): Unit = {
    input.read.fold(onError(error), temperatures => {
      output.write(Measurement(host, now(), temperatures)).leftMap(onError(error)); ()
    })
  }
}
