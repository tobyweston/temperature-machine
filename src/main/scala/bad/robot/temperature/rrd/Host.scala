package bad.robot.temperature.rrd

import java.net.InetAddress

import io.circe.generic.semiauto._

object Host {

  def apply(name: String): Host = {
    new Host(trim(name))
  }

  def local = Host(InetAddress.getLocalHost.getHostName)

  implicit val encoder = deriveEncoder[Host]
  implicit val decoder = deriveDecoder[Host]

  // another arbitrary constraint of rrd4j; data source names can only be a max of 20 characters
  private def trim(name: String) = name.take(20 - "-sensor-x".length)
}

case class Host(name: String)