package bad.robot.temperature.server

import argonaut.CodecJson
import bad.robot.temperature.IpAddress
import bad.robot.temperature.rrd.Host

object Connection {
  implicit val codec = CodecJson.derive[Connection]
}

case class Connection(host: Host, ip: IpAddress)