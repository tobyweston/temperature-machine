package bad.robot.temperature.server

import bad.robot.temperature.Files
import cats.data.Kleisli
import cats.effect._
import org.http4s._

object StaticFiles {

  def apply(): HttpService[IO] = Kleisli.apply(request => {
    val location = Files.path
    val target = location + request.uri.path

    StaticFile.fromString[IO](target, Some(request))
  })
}

