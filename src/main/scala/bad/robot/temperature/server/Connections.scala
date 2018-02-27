package bad.robot.temperature.server

import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.{Clock, Instant}

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{Error, IpAddress}
import org.http4s.headers.`X-Forwarded-For`

import scala.collection.concurrent.TrieMap
import scalaz.{\/, \/-}

object Connections {
  def apply(): Connections = new Connections()
}

class Connections {
  private val connections: TrieMap[Connection, Instant] = TrieMap()

  def update(host: Host, forwardedFor: Option[`X-Forwarded-For`]): Error \/ Unit = {
    \/-(forwardedFor.foreach(ip => connections.put(Connection(host, IpAddress(ip.value)), Instant.now)))
  }

  def all = connections.keys.toList

  def allWithin(minutes: Long)(implicit clock: Clock) = {
    connections.filter(within(minutes)).keys.toList
  }
  
  def reset() = connections.clear()
  
  private def within(amount: Long)(implicit clock: Clock): ((Connection, Instant)) => Boolean = {
    case (_, instant) => instant.isAfter(clock.instant().minus(amount, minutes))
  }

}
