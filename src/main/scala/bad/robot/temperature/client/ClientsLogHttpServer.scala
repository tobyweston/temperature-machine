package bad.robot.temperature.client

import java.lang.Math.max
import java.util.concurrent.Executors.newFixedThreadPool

import bad.robot.temperature.server.{LogEndpoint, VersionEndpoint}
import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.IO
import fs2.Stream
import fs2.StreamApp.ExitCode
import org.http4s.server.blaze.BlazeBuilder
import org.http4s.server.middleware.{CORS, GZip}
import cats.implicits._

import scala.concurrent.ExecutionContext

class ClientsLogHttpServer(port: Int) {

  private val DefaultExecutorService: ExecutionContext = {
    ExecutionContext.fromExecutor(
      newFixedThreadPool(max(4, Runtime.getRuntime.availableProcessors), TemperatureMachineThreadFactory("log-server"))
    )
  }

  private def build(): Stream[IO, ExitCode] = {
    import scala.concurrent.ExecutionContext.Implicits.global // todo replace with explicit one

    BlazeBuilder[IO]
      .withExecutionContext(DefaultExecutorService)
      .bindHttp(port, "0.0.0.0")
      .mountService(
        GZip(
          CORS(
            LogEndpoint() <+>
              VersionEndpoint()
          )
        ), "/")
      .serve
  }

}

object ClientsLogHttpServer {
  def apply(port: Int): Stream[IO, ExitCode] = new ClientsLogHttpServer(port).build()
}