package bad.robot.temperature

import argonaut.CodecJson

object IpAddress {
  val regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  val isIpAddress: (String) => Boolean = address => regex.findFirstMatchIn(address).isDefined

  implicit val codec = CodecJson.derive[IpAddress]
}

case class IpAddress(value: String)