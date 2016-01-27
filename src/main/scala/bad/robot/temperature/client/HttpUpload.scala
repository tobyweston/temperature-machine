package bad.robot.temperature.client

import java.net.InetAddress

import bad.robot.temperature.{Temperature, TemperatureWriter}

case class HttpUpload(address: InetAddress) extends TemperatureWriter {
  def write(temperatures: List[Temperature]) = {
    println(s"I'm uploading $temperatures to $address")
  }
}
