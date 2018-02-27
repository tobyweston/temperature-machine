package bad.robot.temperature.server

import java.time.Clock
import bad.robot.temperature._
import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._

object ConnectionsEndpoint {
  
  private implicit val encoder = jsonEncoder[List[Connection]]
  
  def apply(connections: Connections)(implicit clock: Clock) = HttpService[IO] {
    case GET -> Root / "connections" => {
      Ok(connections.all)
    }

    case GET -> Root / "connections" / "active" / "within" / LongVar(period) / "mins" => {
      Ok(connections.allWithin(period))
    }
  }

}