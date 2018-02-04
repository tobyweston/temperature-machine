package bad.robot.temperature.server

import io.circe.generic.semiauto._
import bad.robot.temperature.IpAddress
import bad.robot.temperature.rrd.Host
import io.circe.{Decoder, ObjectEncoder}

object Connection {
  implicit val encoder: ObjectEncoder[Connection] = deriveEncoder[Connection]
  implicit val decoder: Decoder[Connection] = deriveDecoder[Connection]
}

case class Connection(host: Host, ip: IpAddress)