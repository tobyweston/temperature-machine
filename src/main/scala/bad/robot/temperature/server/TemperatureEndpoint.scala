package bad.robot.temperature.server

import java.time.Clock
import java.time.temporal.ChronoUnit.{MINUTES => minutes}
import java.time.temporal.TemporalUnit

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{jsonEncoder, _}
import cats.effect.IO
import io.circe._
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`

import scala.collection.concurrent.TrieMap

object TemperatureEndpoint {

  private implicit val encoder = jsonEncoder[Json]
  private implicit val decoder = jsonDecoder[Measurement]

  implicit def jsonMapEncoder: Encoder[TrieMap[Host, Measurement]] = new Encoder[TrieMap[Host, Measurement]] {
    def apply(measurements: TrieMap[Host, Measurement]): Json = Json.obj(
      ("measurements", Encoder[List[Measurement]].apply(measurements.values.toList))
    )
  }


  private val temperatures: TrieMap[Host, Measurement] = TrieMap()

  def apply(sensors: TemperatureReader)(implicit clock: Clock) = HttpService[IO] {
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(temperatures => {
        Ok(s"${temperatures.average.temperature.asCelsius}")
      })
    }

    case GET -> Root / "temperatures" / "average" => {
      val average = temperatures.filter(within(5, minutes)).map { case (host, measurement) => {
        host -> measurement.copy(temperatures = List(measurement.temperatures.average))
      }}
      Ok(encode(average))
    }

    case GET -> Root / "temperatures" => {
      Ok(encode(temperatures.filter(within(5, minutes))))
    }

    case DELETE -> Root / "temperatures" => {
      temperatures.clear()
      NoContent()
    }

    case request @ PUT -> Root / "temperature" => {
      request.decode[Measurement](measurement => {
        val result = ConnectionsEndpoint.update(measurement.host, request.headers.get(`X-Forwarded-For`))
        
        result.toHttpResponse(_ => {
          temperatures.put(measurement.host, measurement)
          NoContent()
        })
      })
    }
  }

  private def within(amount: Long, unit: TemporalUnit)(implicit clock: Clock): ((Host, Measurement)) => Boolean = {
    case (_, measurement) => measurement.time.isAfter(clock.instant().minus(amount, unit))
  }

}