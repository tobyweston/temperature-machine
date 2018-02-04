package bad.robot.temperature.server

import bad.robot.temperature._
import cats.effect.IO
import io.circe.Json
import org.http4s.HttpService
import org.http4s.dsl.io._


object VersionEndpoint {

  private val version = Json.obj(
    ("name", Json.fromString(BuildInfo.name)),
    ("version", Json.fromString(BuildInfo.version)),
    ("latestSha", Json.fromString(BuildInfo.latestSha)),
    ("scalaVersion", Json.fromString(BuildInfo.scalaVersion)),
    ("sbtVersion", Json.fromString(BuildInfo.sbtVersion))
  )
  
  def apply() = HttpService[IO] {
    case GET -> Root / "version" => Ok(version)(implicitly, jsonEncoder)
  }
  
}
