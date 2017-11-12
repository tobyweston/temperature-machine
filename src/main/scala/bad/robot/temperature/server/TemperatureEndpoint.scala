package bad.robot.temperature.server

import java.time.Clock
import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.temporal.TemporalUnit

import argonaut.Argonaut._
import argonaut.EncodeJson
import bad.robot.temperature._
import bad.robot.temperature.rrd.Host
import org.http4s.HttpService
import org.http4s.dsl._
import org.http4s.headers.`X-Forwarded-For`
import org.http4s.server.websocket._
import org.http4s.websocket.WebsocketBits._

import scala.concurrent.duration._
import scala.language.postfixOps
import scalaz.concurrent.{Strategy, Task}
import scalaz.stream.time.awakeEvery
import scalaz.stream.{DefaultScheduler, Exchange, Process, Sink}

object TemperatureEndpoint {

  implicit def jsonEncoder: EncodeJson[Map[Host, Measurement]] = {
    EncodeJson((measurements: Map[Host, Measurement]) =>
      argonaut.Json(
        "measurements" := measurements.values.toList
      )
    )
  }

  private var current: Map[Host, Measurement] = Map()

  def service(sensors: TemperatureReader, writer: TemperatureWriter)(implicit clock: Clock) = HttpService {
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(temperatures => {
        Ok(s"${temperatures.average.temperature.asCelsius}")
      })
    }

    case GET -> Root / "temperatures" / "average" => {
      val average = current.filter(within(5, minutes)).map { case (host, measurement) => {
        host -> measurement.copy(temperatures = List(measurement.temperatures.average))
      }}
      Ok(encode(average).spaces2)
    }

    case GET -> Root / "temperatures" => {
      Ok(encode(current.filter(within(5, minutes))).spaces2)
    }

    case GET -> Root / "temperatures" / "live" => {
      val source = awakeEvery(500 millis)(Strategy.DefaultStrategy, DefaultScheduler).map { d => 
        Text(encode(current.filter(within(5, minutes))).spaces2) 
      }

      val sink: Sink[Task, WebSocketFrame] = Process.constant {
        case Text(fromClient, _) => Task.delay(println(fromClient))
        case f                   => Task.delay(println(s"Unknown type: $f"))
      }
      WS(Exchange(source, sink))
    }

    case DELETE -> Root / "temperatures" => {
      current = Map[Host, Measurement]()
      NoContent()
    }

    case request @ PUT -> Root / "temperature" => {
      val json = request.as[String].unsafePerformSync
      val result = for {
        measurement <- decode[Measurement](json)
        _           <- writer.write(measurement)
        _           <- ConnectionsEndpoint.update(measurement.host, request.headers.get(`X-Forwarded-For`))
      } yield measurement
      result.toHttpResponse(success => {
        current = current + (success.host -> success)
        NoContent()
      })
    }
  }

  private def within(amount: Long, unit: TemporalUnit)(implicit clock: Clock): ((Host, Measurement)) => Boolean = {
    case (_, measurement) => measurement.time.isAfter(clock.instant().minus(amount, unit))
  }

}