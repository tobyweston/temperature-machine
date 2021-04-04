package bad.robot.temperature.server

import cats.data.Kleisli
import cats.effect.{ContextShift, IO}
import org.http4s.CacheDirective.{`max-age`, `public`}
import org.http4s.HttpRoutes
import org.http4s.Status.Successful
import org.http4s.headers.`Cache-Control`
import org.http4s.server.staticcontent._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object StaticResources {
  def apply()(implicit cs: ContextShift[IO]): HttpRoutes[IO] = Kleisli { request =>
    val routes = resourceService[IO](
      ResourceService.Config(
        basePath = "",
        blockingExecutionContext = ExecutionContext.global,
        cacheStrategy = MemoryCache()
      ))

    val response =
      if (request.uri.path.endsWith("/"))
        routes(request.withUri(request.uri.withPath(request.uri.path + "index.html")))
      else
        routes(request)

    response.map {
      case Successful(resp) => resp.putHeaders(`Cache-Control`(`public`, `max-age`(Duration(365, DAYS))))
      case resp             => resp
    }
  }

}