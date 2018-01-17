package bad.robot.temperature.server

import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.{Clock, Instant}

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{Error, _}
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`

import scala.collection.concurrent.TrieMap
import scalaz.{\/, \/-}

object ConnectionsEndpoint {

  private val connections: TrieMap[Connection, Instant] = TrieMap()

  def update(host: Host, forwardedFor: Option[`X-Forwarded-For`]): Error \/ Unit = {
    \/-(forwardedFor.foreach(ip => connections.put(Connection(host, IpAddress(ip.value)), Instant.now)))
  }

  def reset() = connections.clear()

  def apply(implicit clock: Clock) = HttpService[IO] {
    case GET -> Root / "connections" => {
      Ok(connections.keys.toList)
    }

    case GET -> Root / "connections" / "active" / "within" / LongVar(period) / "mins" => {
      Ok(connections.filter(within(period)).keys.toList)
    }
  }

  private def within(amount: Long)(implicit clock: Clock): ((Connection, Instant)) => Boolean = {
    case (_, instant) => instant.isAfter(clock.instant().minus(amount, minutes))
  }
}