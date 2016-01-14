package bad.robot.temperature.server

import bad.robot.temperature.readingToTemperature
import bad.robot.temperature.FailedToFindFile
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import org.http4s.dsl._
import org.http4s.server.HttpService

import scalaz.syntax.std.option._

object CurrentTemperature {

  private val sensor = SensorFile.find().headOption

  def service = HttpService {

    case GET -> Root / "temperature" => {
      val result = for {
        file        <- sensor.toRightDisjunction(FailedToFindFile(BaseFolder))
        temperature <- SensorReader(List(file)).read
      } yield temperature.head

      result.toHttpResponse(reading => Ok(f"${reading.celsius}%.1f °C"))
    }

  }

}