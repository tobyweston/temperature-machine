package bad.robot.temperature.server

import java.lang.Math._
import java.time.Clock
import java.util.concurrent.Executors._
import java.util.concurrent.{CountDownLatch, ExecutorService}

import bad.robot.temperature.JsonToCsv
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.implicits._
//import org.http4s.implicits._
import cats.effect.IO
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Server => Http4sServer}

object HttpServer {
  def apply(port: Int, monitored: List[Host], current: CurrentTemperatures, all: AllTemperatures): IO[HttpServer] = IO {
    val server = new HttpServer(port, monitored)
    server.build(current, all).unsafeRunSync
    server
  }
}

class HttpServer(port: Int, monitored: List[Host]) {

  private val latch = new CountDownLatch(1)

  def awaitShutdown(): IO[Unit] = IO(latch.await())

  def shutdown(): IO[Unit] = IO(latch.countDown())

  private val DefaultExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("http-server"))
  }

  private def build(current: CurrentTemperatures, all: AllTemperatures): IO[Http4sServer[IO]] = BlazeBuilder[IO]
    .withExecutionContext(scala.concurrent.ExecutionContext.fromExecutorService(DefaultExecutorService))
    .bindHttp(port, "0.0.0.0")
    .mountService(services(current, all), "/")
    .start

  private def services(current: CurrentTemperatures, all: AllTemperatures): HttpService[IO] = {
    CORS(
      TemperatureEndpoint(SensorReader(Host.local, SensorFile.find()), current, all) <+>
      ConnectionsEndpoint(Clock.systemDefaultZone) <+>
      LogEndpoint() <+>
      ExportEndpoint(JsonFile.load, JsonToCsv.DefaultTimeFormatter) <+>
      VersionEndpoint() <+>
      StaticFiles() <+>
      StaticResources()
    )
  }
}