package bad.robot.temperature.server

import bad.robot.temperature.rrd.RrdFile
import org.http4s.server.staticcontent._
import org.http4s.{HttpService, Response, Service, StaticFile}
object TemperatureResources {

  def service: HttpService = Service.lift(request => {
    val file = RrdFile.path + request.uri.path
    StaticFile.fromString(file).fold(Response.fallthrough)(NoopCacheStrategy.cache(request.pathInfo, _))
  })
}

