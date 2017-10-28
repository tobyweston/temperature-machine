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
      Ok(average)
    }

    case GET -> Root / "temperatures" => {
      Ok(current.filter(within(5, minutes)))
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