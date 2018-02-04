package bad.robot.temperature.server

import bad.robot.temperature.rrd.RrdFile
import cats.data.Kleisli
import cats.effect._
import org.http4s._

object ApplicationHomeFiles {

  def apply(): HttpService[IO] = Kleisli.apply(request => {
    val location = RrdFile.path
    val target = location + request.uri.path

    StaticFile.fromString[IO](target, Some(request))
  })
}

