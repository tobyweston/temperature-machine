package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{Measurement, Temperature, TemperatureWriter}

import scalaz.\/-

case class HttpUpload(address: InetAddress) extends TemperatureWriter {
  def write(measurement: Measurement) = {
    println(s"I'm uploading $measurement to $address")
    \/-(Unit)
  }
}
