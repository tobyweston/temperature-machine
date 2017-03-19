package bad.robot.temperature

import argonaut.CodecJson

object IpAddress {
  implicit val codec = CodecJson.derive[IpAddress]
}

case class IpAddress(value: String)