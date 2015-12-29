package bad.robot.temperature.server

import org.http4s.CacheDirective.{`max-age`, `no-cache`, `no-store`}
import org.http4s.headers.`Cache-Control`
import org.http4s.server.staticcontent._
import org.http4s.server.{HttpService, Service}

import scala.concurrent.duration.Duration._

object StaticResources {
  def service: HttpService = Service.lift(request => {
    val resources = resourceService(ResourceService.Config(""))

    val response = if (request.uri.path.endsWith("/"))
      resources(request.copy(uri = request.uri.withPath(request.uri.path + "index.html")))
    else
      resources(request)

    response.map(_.putHeaders(`Cache-Control`(`no-cache`(), `no-store`, `max-age`(Zero))))
  })
}