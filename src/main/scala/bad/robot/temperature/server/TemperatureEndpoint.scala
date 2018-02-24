package bad.robot.temperature.server

import java.time.Clock

import bad.robot.temperature.rrd.Host
import bad.robot.temperature.{jsonEncoder, _}
import cats.effect.IO
import io.circe._
import org.http4s.HttpService
import org.http4s.dsl.io._
import org.http4s.headers.`X-Forwarded-For`

object TemperatureEndpoint {

  private implicit val encoder = jsonEncoder[Json]
  private implicit val decoder = jsonDecoder[Measurement]

  implicit def jsonMapEncoder: Encoder[Map[Host, Measurement]] = new Encoder[Map[Host, Measurement]] {
    def apply(measurements: Map[Host, Measurement]): Json = Json.obj(
      ("measurements", Encoder[List[Measurement]].apply(measurements.values.toList))
    )
  }

  private val lastestTemperatures = CurrentTemperatures(Clock.systemDefaultZone)

  def apply(sensors: TemperatureReader, allTemperatures: AllTemperatures) = HttpService[IO] {
    // todo delete this one, it shouldn't be used
    case GET -> Root / "temperature" => {
      sensors.read.toHttpResponse(measurement => {
        Ok(s"${measurement.temperatures.average.temperature.asCelsius}")
      })
    }

    case GET -> Root / "temperatures" / "average" => {
      Ok(encode(lastestTemperatures.average))
    }

    case GET -> Root / "temperatures" => {
      Ok(encode(lastestTemperatures.all))
    }

    case DELETE -> Root / "temperatures" => {
      lastestTemperatures.clear()
      NoContent()
    }

    case request @ PUT -> Root / "temperature" => {
      request.decode[Measurement](measurement => {
        val result = ConnectionsEndpoint.update(measurement.host, request.headers.get(`X-Forwarded-For`))
        
        result.toHttpResponse(_ => {
          lastestTemperatures.updateWith(measurement)
          allTemperatures.put(measurement)
          NoContent()
        })
      })
    }
  }

}