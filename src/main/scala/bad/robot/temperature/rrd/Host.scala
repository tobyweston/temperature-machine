package bad.robot.temperature.rrd

import java.net.InetAddress
import java.time.{Instant, ZoneId}

import io.circe.generic.semiauto._

object Host {

  private val localUtcOffset = Some(ZoneId.systemDefault().getRules.getOffset(Instant.now()).getId)

  def apply(name: String, utcOffset: Option[String]): Host = {
    new Host(trim(name), utcOffset)
  }

  def local = Host(InetAddress.getLocalHost.getHostName, localUtcOffset)
  
  implicit val encoder = deriveEncoder[Host]
  implicit val decoder = deriveDecoder[Host]

  // another arbitrary constraint of rrd4j; data source names can only be a max of 20 characters
  def trim(name: String) =name.take(20 - "-sensor-x".length)
}

case class Host(name: String, utcOffset: Option[String])