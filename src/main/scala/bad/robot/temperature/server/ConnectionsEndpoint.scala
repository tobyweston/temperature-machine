package bad.robot.temperature.server

import java.net.InetAddress
import java.time.Clock

import bad.robot.temperature.IpAddress.isIpAddress
import bad.robot.temperature._
import bad.robot.temperature.client.HttpUpload.allNetworkInterfaces
import cats.data.NonEmptyList
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._

import scala.collection.JavaConverters._
import scalaz.Scalaz._

object ConnectionsEndpoint {

  private implicit val encoder = jsonEncoder[List[Connection]]

  def apply(connections: Connections)(implicit clock: Clock) = HttpService[IO] {
    case GET -> Root / "connections" => {
      Ok(connections.all).map(_.putHeaders(xForwardedHost(currentIpAddress)))
    }

    case GET -> Root / "connections" / "active" / "within" / LongVar(period) / "mins" => {
      Ok(connections.allWithin(period)).map(_.putHeaders(xForwardedHost(currentIpAddress)))
    }
  }

  private def xForwardedHost(ipAddresses: NonEmptyList[Option[InetAddress]]): Header = {
    Header("X-Forwarded-Host", currentIpAddress.map(_.fold("unknown")(_.getHostAddress)).mkString(", "))
  }

  private val currentIpAddress: NonEmptyList[Option[InetAddress]] = {
    val addresses = for {
      interface <- allNetworkInterfaces
      address   <- interface.getInetAddresses.asScala
      ip        <- isIpAddress(address.getHostAddress).option(address)
    } yield Some(ip)

    NonEmptyList(addresses.head, addresses.tail)
  }

}