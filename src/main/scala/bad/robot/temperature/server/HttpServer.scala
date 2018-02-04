package bad.robot.temperature.server

import java.lang.Math._
import java.time.Clock
import java.util.concurrent.Executors._
import java.util.concurrent.{CountDownLatch, ExecutorService}

import bad.robot.temperature.{ErrorOnTemperatureSpike, JsonToCsv}
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Host, Rrd}
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.implicits._
//import org.http4s.implicits._
import cats.effect.IO
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS
import org.http4s.server.{Server => Http4sServer}

object HttpServer {
  def apply(port: Int, monitored: List[Host]): IO[HttpServer] = IO.pure {
    val server = new HttpServer(port, monitored)
    server.build().unsafeRunSync
    server
  }
}

class HttpServer(port: Int, monitored: List[Host]) {

  private val latch = new CountDownLatch(1)

  def awaitShutdown(): IO[Unit] = IO.pure(latch.await())

  def shutdown(): IO[Unit] = IO.pure(latch.countDown())

  private val DefaultExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("http-server"))
  }

  private def build(): IO[Http4sServer[IO]] = BlazeBuilder[IO]
    .withExecutionContext(scala.concurrent.ExecutionContext.fromExecutorService(DefaultExecutorService))
    .bindHttp(port, "0.0.0.0")
    .mountService(services(), "/")
    .start

  private def services(): HttpService[IO] = {
    CORS(
      TemperatureEndpoint(SensorReader(SensorFile.find()), ErrorOnTemperatureSpike(Rrd(monitored)))(Clock.systemDefaultZone) <+>
      ConnectionsEndpoint(Clock.systemDefaultZone) <+>
      LogEndpoint() <+>
      ExportEndpoint(JsonFile.load, JsonToCsv.DefaultTimeFormatter) <+>
      VersionEndpoint() <+>
      ApplicationHomeFiles() <+>
      StaticResources()
    )
  }
}