package bad.robot.temperature.server

import java.lang.Math._
import java.time.Clock
import java.util.concurrent.Executors._
import java.util.concurrent.{CountDownLatch, ExecutorService}

import bad.robot.temperature.JsonToCsv
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.Host
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import fs2.Scheduler
import fs2.StreamApp._
import cats.implicits._

import scala.concurrent.ExecutionContext
//import org.http4s.implicits._
import cats.effect.IO
import org.http4s.HttpService
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.CORS

object HttpServer {
  def apply(port: Int, monitored: List[Host], temperatures: AllTemperatures, connections: Connections): IO[HttpServer] = IO {
    val server = new HttpServer(port, monitored)
    server.build(temperatures, connections) // this has gotten well hokey, prob shouldn't return this as an IO but Stream
    server
  }
}

class HttpServer(port: Int, monitored: List[Host]) {

  private val latch = new CountDownLatch(1)

  def awaitShutdown(): IO[Unit] = IO(latch.await())

  def shutdown(): IO[Unit] = IO(latch.countDown())

  private val DefaultHttpExecutorService: ExecutorService = {
    newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("http-server"))
  }

  private def build(temperatures: AllTemperatures, connections: Connections): fs2.Stream[IO, ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global // todo replace with explicit one
    
    for {
      scheduler <- Scheduler[IO](corePoolSize = 1)
      exitCode  <- BlazeBuilder[IO]
                    .withWebSockets(true)
                    .withExecutionContext(ExecutionContext.fromExecutorService(DefaultHttpExecutorService))
                    .bindHttp(port, "0.0.0.0")
                    .mountService(services(scheduler, temperatures, connections), "/")
                    .serve
    } yield exitCode
  }

  private def services(scheduler: Scheduler, temperatures: AllTemperatures, connections: Connections): HttpService[IO] = {
    CORS(
      TemperatureEndpoint(scheduler, SensorReader(Host.local, SensorFile.find()), temperatures, connections) <+>
      ConnectionsEndpoint(connections)(Clock.systemDefaultZone) <+>
      LogEndpoint() <+>
      ExportEndpoint(JsonFile.load, JsonToCsv.DefaultTimeFormatter) <+>
      VersionEndpoint() <+>
      StaticFiles() <+>
      StaticResources()
    )
  }
}