package bad.robot.temperature.server

import bad.robot.temperature._
import org.http4s.dsl._
import org.specs2.mutable.Specification

import scalaz.{-\/, \/-}

class EitherToResponseTest extends Specification {

  val okResponse = (_: Any) => Ok()

  "Error mappings" >> {
    -\/(CrcFailure()).toHttpResponse(okResponse).unsafePerformSync.status                    must_== InternalServerError
    -\/(SensorError("???")).toHttpResponse(okResponse).unsafePerformSync.status              must_== InternalServerError
    -\/(UnexpectedError("???")).toHttpResponse(okResponse).unsafePerformSync.status          must_== InternalServerError
    -\/(FailedToFindFile("???")).toHttpResponse(okResponse).unsafePerformSync.status         must_== NotFound
    -\/(FileError(new Exception("???"))).toHttpResponse(okResponse).unsafePerformSync.status must_== InternalServerError
  }

  "Success mapping" >> {
    \/-("Well done").toHttpResponse(okResponse).unsafePerformSync.status must_== Ok
  }
}
