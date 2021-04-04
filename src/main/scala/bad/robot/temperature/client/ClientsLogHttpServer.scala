package bad.robot.temperature.client

import java.lang.Math.max
import java.util.concurrent.Executors.newFixedThreadPool

import bad.robot.temperature.server.{LogEndpoint, VersionEndpoint}
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.{ConcurrentEffect, ExitCode, IO, Timer}
import cats.implicits._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.{CORS, GZip}

import scala.concurrent.ExecutionContext

class ClientsLogHttpServer(port: Int) {

  private val DefaultExecutorService: ExecutionContext = {
    ExecutionContext.fromExecutor(
      newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("log-server"))
    )
  }

  private def build(implicit F: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = {
    BlazeServerBuilder[IO]
      .withExecutionContext(DefaultExecutorService)
      .bindHttp(port, "0.0.0.0")
      .withHttpApp(
        GZip(
          CORS(
            LogEndpoint() <+>
            VersionEndpoint()
          )
        ).orNotFound
      )
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

}

object ClientsLogHttpServer {
  def apply(port: Int)(implicit F: ConcurrentEffect[IO], timer: Timer[IO]): IO[ExitCode] = new ClientsLogHttpServer(port).build
}