package bad.robot.temperature.server

import java.lang.Math._
import java.time.Clock
import java.util.concurrent.{ExecutorService, Executors}
import java.util.concurrent.Executors._

import bad.robot.temperature.JsonToCsv
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.{ConcurrentEffect, ExitCode, IO, Timer, _}
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, GZip}
import org.http4s.server.staticcontent.{FileService, MemoryCache, ResourceService, resourceService, fileService}

import scala.concurrent.ExecutionContext

object HttpServer {
  def apply(port: Int, monitored: List[Host])(implicit F: ConcurrentEffect[IO], timer: Timer[IO]): HttpServer = {
    new HttpServer(port, monitored)
  }
}

class HttpServer(port: Int, monitored: List[Host]) {

  private val DefaultHttpExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("http-server"))
  }
  
  def build(temperatures: AllTemperatures, connections: Connections)(implicit F: ConcurrentEffect[IO], timer: Timer[IO], cs: ContextShift[IO]): IO[ExitCode] = {
    val endpoints = GZip(
      CORS(
        TemperatureEndpoint(SensorReader(Host.local, SensorFile.find()), temperatures, connections) <+>
          ConnectionsEndpoint(connections)(Clock.systemDefaultZone) <+>
          LogEndpoint() <+>
          ExportEndpoint(JsonFile.load, JsonToCsv.DefaultTimeFormatter) <+>
          VersionEndpoint() <+>
//          resourceService[IO](ResourceService.Config(basePath = "", ExecutionContext.global, cacheStrategy = MemoryCache()))
          StaticFiles() <+>
          StaticResources()
      )
    )
    BlazeServerBuilder[IO]
      .withWebSockets(true)
      .withExecutionContext(ExecutionContext.fromExecutorService(DefaultHttpExecutorService))
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(endpoints.orNotFound)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
  
}