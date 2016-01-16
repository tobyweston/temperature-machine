package bad.robot.temperature.server

import bad.robot.temperature.CrcFailure
import org.http4s.dsl._
import org.specs2.mutable.Specification

import scalaz.-\/

class EitherToResponseTest extends Specification {

  "Error mappings" >> {
    -\/(CrcFailure).toHttpResponse(ko)
    -\/(CrcFailure).toHttpResponse(temperatures => {
      Ok()
    })
  }

}
