package bad.robot.temperature.server

import bad.robot.temperature.rrd.RrdFile
import cats.effect._
import org.http4s._
import org.http4s.dsl.io._

object ApplicationHomeFiles {

//  val serveOrPassthrough: Request[IO] => IO[Response[IO]] = request => {
//    val location = RrdFile.path
//    val target = location + request.uri.path
//    StaticFile.fromString[IO](target, Some(request)).fold {
//      // passthrough
//      IO.pure(???)
//    } {
//      // process request
//      response => NoopCacheStrategy[IO].cache(request.pathInfo, response)
//    }
//  }

  def apply(): HttpService[IO] = HttpService {
    case request @ GET -> Root / "temperature.json" =>
      StaticFile.fromString(RrdFile.path + "temperature.json", Some(request)).getOrElseF(NotFound())
  }
}

