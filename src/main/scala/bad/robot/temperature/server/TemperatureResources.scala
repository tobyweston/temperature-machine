package bad.robot.temperature.server

import bad.robot.temperature.rrd.RrdFile
import org.http4s.StaticFile
import org.http4s.server.staticcontent._
import org.http4s.server.{HttpService, _}
object TemperatureResources {

  def service: HttpService = Service.lift(request => {
    val file = RrdFile.path + request.uri.path
    StaticFile.fromString(file).fold(HttpService.notFound)(NoopCacheStrategy.cache(request.pathInfo, _))
  })
}

