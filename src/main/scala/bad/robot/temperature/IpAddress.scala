package bad.robot.temperature

import io.circe.generic.semiauto._

object IpAddress {
  val regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  val isIpAddress: (String) => Boolean = address => regex.findFirstMatchIn(address).isDefined

  implicit val encoder = deriveEncoder[IpAddress]
  implicit val decoder = deriveDecoder[IpAddress]
}

case class IpAddress(value: String)