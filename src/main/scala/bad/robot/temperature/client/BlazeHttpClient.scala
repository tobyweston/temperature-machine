package bad.robot.temperature.client

import java.lang.Runtime.getRuntime
import java.util.concurrent._

import bad.robot.temperature.task.TemperatureMachineThreadFactory
import org.http4s.client.asynchttpclient.AsyncHttpClient
import org.http4s.client.asynchttpclient.AsyncHttpClient.{defaultConfig => AysncHttpClientDefaultConfig}
import org.http4s.client.blaze.BlazeClientConfig.{defaultConfig => BlazeDefaultConfig}
import org.http4s.client.blaze.PooledHttp1Client

import scala.concurrent.duration._

object BlazeHttpClient {

  private val DefaultTimeout: Duration = 5.minutes
  private val DefaultExecutor = {
    new ThreadPoolExecutor(1, getRuntime.availableProcessors() * 6, 60L, SECONDS, new LinkedBlockingQueue[Runnable](), TemperatureMachineThreadFactory("blaze-http-client"))
  }

  def apply() = PooledHttp1Client(
    config = BlazeDefaultConfig.copy(idleTimeout = DefaultTimeout, customExecutor = Some(DefaultExecutor)
  ))
}

object AltHttpClient {
  private val DefaultExecutor = {
    new ThreadPoolExecutor(1, getRuntime.availableProcessors() * 6, 60L, SECONDS, new LinkedBlockingQueue[Runnable](), TemperatureMachineThreadFactory("async-http-client"))
  }
  
  def apply() = AsyncHttpClient(
    config = AysncHttpClientDefaultConfig, customExecutor = Some(DefaultExecutor)
  ) 
}