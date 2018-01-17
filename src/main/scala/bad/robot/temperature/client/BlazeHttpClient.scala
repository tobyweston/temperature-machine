package bad.robot.temperature.client

import java.lang.Runtime.getRuntime
import java.util.concurrent._

import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.IO
import org.http4s.client.{Client => Http4sClient}
import org.http4s.client.blaze.BlazeClientConfig._
import org.http4s.client.blaze.Http1Client

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

object BlazeHttpClient {

  private val DefaultTimeout: Duration = 5.minutes
  private val DefaultExecutor = {
    ExecutionContext.fromExecutor(
      new ThreadPoolExecutor(1, getRuntime.availableProcessors() * 6, 60L, SECONDS, new LinkedBlockingQueue[Runnable](), TemperatureMachineThreadFactory("client"))
    )
  }

  // convert to Http1Client.stream[F[_]: Effect]: Stream[F, Http1Client] ?
  def apply(): Http4sClient[IO] = Http1Client[IO](
    config = defaultConfig.copy(idleTimeout = DefaultTimeout, executionContext = DefaultExecutor
  )).unsafeRunSync()
}
