package bad.robot.temperature.server

import bad.robot.temperature.Files
import cats.data.Kleisli
import cats.effect._
import org.http4s._

import scala.concurrent.ExecutionContext

object StaticFiles {

  def apply()(implicit cs: ContextShift[IO]): HttpRoutes[IO] = Kleisli.apply(request => {
    val location = Files.path
    val target = location + request.uri.path

    StaticFile.fromString[IO](target, ExecutionContext.global, Some(request))
  })
}

