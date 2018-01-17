package bad.robot.temperature.server

import cats.effect.IO
import org.http4s.CacheDirective.{`max-age`, `no-cache`, `no-store`}
import org.http4s.Status.Successful
import org.http4s.headers.`Cache-Control`
import org.http4s.server.staticcontent._
import org.http4s.{HttpService, Service}

import scala.concurrent.duration.Duration._

object StaticResources {
  def apply(): HttpService[IO] = HttpService.lift(request => {
    val resources = resourceService[IO](ResourceService.Config(basePath = ""))

    val response = if (request.uri.path.endsWith("/"))
      resources(request.withUri(request.uri.withPath(request.uri.path + "index.html")))
    else
      resources(request)

    response.map {
      case Successful(resp) => resp.putHeaders(`Cache-Control`(`no-cache`(), `no-store`, `max-age`(Zero)))
      case resp             => resp
    }
  })
}