package bad.robot.temperature.server

import cats.data.Kleisli
import cats.effect.IO
import org.http4s.CacheDirective.{`max-age`, `public`}
import org.http4s.HttpService
import org.http4s.Status.Successful
import org.http4s.headers.`Cache-Control`
import org.http4s.server.staticcontent._

import scala.concurrent.duration._

object StaticResources {
  def apply(): HttpService[IO] = Kleisli.apply(request => {
    val resources = resourceService[IO](ResourceService.Config(basePath = ""))

    val response = if (request.uri.path.endsWith("/"))
      resources(request.withUri(request.uri.withPath(request.uri.path + "index.html")))
    else
      resources(request)

    response.map {
      case Successful(resp) => resp.putHeaders(`Cache-Control`(`public`, `max-age`(Duration(31536000, SECONDS))))
      case resp             => resp
    }
  })

}