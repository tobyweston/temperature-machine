package bad.robot.temperature.client

import java.lang.Runtime.getRuntime
import java.util.concurrent._

import bad.robot.temperature.task.TemperatureMachineThreadFactory
import cats.effect.{ConcurrentEffect, IO, Resource}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.{Client => Http4sClient}

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
  def apply()(implicit F: ConcurrentEffect[IO]): Resource[IO, Http4sClient[IO]] = BlazeClientBuilder[IO](DefaultExecutor)
    .withIdleTimeout(DefaultTimeout)
    .resource

}
