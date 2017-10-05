package bad.robot.temperature.server

import bad.robot.temperature.rrd.RrdFile
import org.http4s.server.staticcontent._
import org.http4s._

object StaticFiles {

  def service: HttpService = Service.lift(request => {
    val file = RrdFile.path + request.uri.path
    StaticFile.fromString(file).fold {
      Pass.now  // aka fallthrough
    } {
      NoopCacheStrategy.cache(request.pathInfo, _)
    }
  })
}

