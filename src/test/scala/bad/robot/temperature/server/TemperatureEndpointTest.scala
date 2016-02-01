package bad.robot.temperature.server

import bad.robot.temperature.{Error, Temperature, TemperatureReader}
import org.http4s.Method.PUT
import org.http4s.dsl._
import org.http4s.{Request, Uri}
import org.specs2.mutable.Specification

import scalaz.{\/, \/-}

class TemperatureEndpointTest extends Specification {

  "Put some temperature data" >> {
    val request = Request(PUT, Uri(path = s"temperature")).withBody("body").run
    val response = TemperatureEndpoint.service(stub(\/-(List()))).apply(request).run
    response must haveStatus(InternalServerError)
  }

  def stub(result: Error \/ List[Temperature]) = new TemperatureReader {
    def read: Error \/ List[Temperature] = result
  }

}
