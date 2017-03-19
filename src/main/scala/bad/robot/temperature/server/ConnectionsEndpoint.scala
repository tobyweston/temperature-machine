package bad.robot.temperature.server

import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.{Clock, Instant}

import bad.robot.temperature.{Error, _}
import org.http4s.HttpService
import org.http4s.dsl._
import org.http4s.headers.`X-Forwarded-For`

import scala.collection.concurrent.TrieMap
import scalaz.{\/, \/-}

object ConnectionsEndpoint {

  private val connections: TrieMap[Ip, Instant] = TrieMap()

  def update(forwardedFor: Option[`X-Forwarded-For`]): Error \/ Unit = {
    \/-(forwardedFor.foreach(ip => connections.put(ip.value, Instant.now)))
  }

  def reset() = connections.clear()

  def service(implicit clock: Clock) = HttpService {
    case GET -> Root / "connections" => {
      Ok(encode(connections.keys.toList).spaces2)
    }

    case GET -> Root / "connections" / "active" / "within" / LongVar(period) / "mins" => {
      Ok(encode(connections.filter(within(period)).keys.toList).spaces2)
    }
  }

  private def within(amount: Long)(implicit clock: Clock): ((Ip, Instant)) => Boolean = {
    case (_, instant) => instant.isAfter(clock.instant().minus(amount, minutes))
  }

}