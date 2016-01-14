package bad.robot.temperature.server

import bad.robot.temperature.FailedToFindFile
import bad.robot.temperature.Temperature
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import org.http4s.dsl._
import org.http4s.server.HttpService

import scalaz.syntax.std.option._

object CurrentAverageTemperature {

  private val sensor = SensorFile.find().headOption

  private val average: (List[Temperature]) => Temperature = (temperatures) => {
    temperatures.fold(Temperature(0.0))(_ + _) / temperatures.length
  }

  def service = HttpService {

    case GET -> Root / "temperature" => {
      val result = for {
        file        <- sensor.toRightDisjunction(FailedToFindFile(BaseFolder))
        temperature <- SensorReader(List(file)).read
      } yield temperature

      result.toHttpResponse(temperatures => Ok(f"${average(temperatures).celsius}%.1f °C"))
    }
  }

}