package bad.robot.temperature

import java.net.InetAddress

import bad.robot.temperature.client.HttpUpload.allNetworkInterfaces
import cats.data.NonEmptyList
import io.circe.generic.semiauto._
import scala.collection.JavaConverters._

object IpAddress {
  val regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r

  val isIpAddress: (String) => Boolean = address => regex.findFirstMatchIn(address).isDefined

  implicit val encoder = deriveEncoder[IpAddress]
  implicit val decoder = deriveDecoder[IpAddress]

  val currentIpAddress: NonEmptyList[Option[InetAddress]] = {
    val addresses = for {
      interface <- allNetworkInterfaces
      address   <- interface.getInetAddresses.asScala
      ip        <- if (isIpAddress(address.getHostAddress)) Some(address) else None
    } yield Some(ip)

    NonEmptyList(addresses.head, addresses.tail)
  }
  
}

case class IpAddress(value: String)