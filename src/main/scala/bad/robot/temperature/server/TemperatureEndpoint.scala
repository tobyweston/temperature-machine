package bad.robot.temperature.server

import java.time.Clock

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{jsonEncoder, _}
import cats.effect.IO
import io.circe._
import fs2.{Sink, _}
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.server.websocket._
import org.http4s.websocket.WebsocketBits._

import scala.concurrent.duration._
import scala.language.postfixOps

object TemperatureEndpoint {

  private implicit val encoder = jsonEncoder[Json]
  private implicit val decoder = jsonDecoder[Measurement]

  implicit def jsonMapEncoder: Encoder[Map[Host, Measurement]] = new Encoder[Map[Host, Measurement]] {
    def apply(measurements: Map[Host, Measurement]): Json = Json.obj(
      ("measurements", Encoder[List[Measurement]].apply(measurements.values.toList))
    )
  }

  private val latestTemperatures = CurrentTemperatures(Clock.systemDefaultZone)
  
  private val sink: Sink[IO, WebSocketFrame] = _.evalMap { (ws: WebSocketFrame) =>
    ws match {
      case Text(fromClient, _) => IO(println(s"Client sent ws data: $fromClient"))
      case frame               => IO(println(s"Unknown type sent from ws client: $frame"))
    }
  }
  
  
  import scala.concurrent.ExecutionContext.Implicits.global // todo replace with explicit one

  def apply(scheduler: Scheduler, sensors: TemperatureReader, allTemperatures: AllTemperatures, connections: Connections) = HttpService[IO] {

    case GET -> Root / "temperatures" / "average" => {
      Ok(encode(latestTemperatures.average))
    }

    case GET -> Root / "temperatures" / "live" / "average" => {
      val source: Stream[IO, WebSocketFrame] = scheduler.awakeEvery[IO](1 second).map { _ =>
        Text(encode(latestTemperatures.average).spaces2ps)
      }
      
      WebSocketBuilder[IO].build(source, sink)
    }

    case GET -> Root / "temperatures" => {
      Ok(encode(latestTemperatures.all))
    }

    case GET -> Root / "temperatures" / "live" / "average" => {
      val source: Stream[IO, WebSocketFrame] = scheduler.awakeEvery[IO](1 second).map { _ =>
        Text(encode(latestTemperatures.all).spaces2ps)
      }

      WebSocketBuilder[IO].build(source, sink)
    }
      
    case DELETE -> Root / "temperatures" => {
      latestTemperatures.clear()
      NoContent()
    }

    case request @ PUT -> Root / "temperature" => {
      request.decode[Measurement](measurement => {
        val result = connections.update(measurement.host, request.headers.get(`X-Forwarded-For`))

        result.toHttpResponse(_ => {
          latestTemperatures.updateWith(measurement)
          allTemperatures.put(measurement)
          NoContent()
        })
      })
    }
  }

}