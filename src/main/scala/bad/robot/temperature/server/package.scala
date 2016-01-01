package bad.robot.temperature

import org.http4s.Response
import org.http4s.dsl._
import java.lang.{Error => _}

import scalaz.\/
import scalaz.concurrent.Task

package object server {

  implicit class EitherToResponse[A](either: Error \/ A) {
    def toHttpResponse(success: A => Task[Response]): Task[Response] = {
      either.fold(errors, success)
    }

    val errors = PartialFunction[Error, Task[Response]] {
      case error @ CrcFailure() => InternalServerError(error.message)
      case error @ FailedToFindFile(_) => NotFound(error.message)
      case error @ FileError(_) => InternalServerError(error.message)
      case error @ SensorError(_) => InternalServerError(error.message)
      case error @ UnexpectedError(_) => InternalServerError(error.message)
    }
  }

}
