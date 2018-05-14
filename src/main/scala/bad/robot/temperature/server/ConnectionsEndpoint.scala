package bad.robot.temperature.server

import java.net.InetAddress
import java.time.Clock

import bad.robot.temperature.IpAddress._
import bad.robot.temperature._
import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._
import org.http4s._
import org.http4s.dsl.io._

object ConnectionsEndpoint {

  private implicit val encoder = jsonEncoder[List[Connection]]

  def apply(connections: Connections, ipAddresses: => NonEmptyList[Option[InetAddress]] = currentIpAddress)(implicit clock: Clock) = HttpService[IO] {
    case GET -> Root / "connections" => {
      Ok(connections.all).map(_.putHeaders(xForwardedHost(ipAddresses)))
    }

    case GET -> Root / "connections" / "active" / "within" / LongVar(period) / "mins" => {
      Ok(connections.allWithin(period)).map(_.putHeaders(xForwardedHost(ipAddresses)))
    }
  }

  private def xForwardedHost(ipAddresses: NonEmptyList[Option[InetAddress]]): Header = {
    Header("X-Forwarded-Host", ipAddresses.map(_.fold("unknown")(_.getHostAddress)).mkString_("", ", ", ""))
  }

}