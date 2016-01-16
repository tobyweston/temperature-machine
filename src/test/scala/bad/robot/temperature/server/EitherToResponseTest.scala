package bad.robot.temperature.server

import java.lang.{Error => _}

import bad.robot.temperature._
import org.http4s.dsl._
import org.specs2.mutable.Specification

import scala.{Error => _}
import scalaz.{-\/, \/-}

class EitherToResponseTest extends Specification {

  val okResponse = (_: Any) => Ok()

  "Error mappings" >> {
    -\/(CrcFailure()).toHttpResponse(okResponse).run.status                    must_== InternalServerError
    -\/(SensorError("???")).toHttpResponse(okResponse).run.status              must_== InternalServerError
    -\/(UnexpectedError("???")).toHttpResponse(okResponse).run.status          must_== InternalServerError
    -\/(FailedToFindFile("???")).toHttpResponse(okResponse).run.status         must_== NotFound
    -\/(FileError(new Exception("???"))).toHttpResponse(okResponse).run.status must_== InternalServerError
  }

  "Success mapping" >> {
    \/-("Well done").toHttpResponse(okResponse).run.status must_== Ok
  }
}
