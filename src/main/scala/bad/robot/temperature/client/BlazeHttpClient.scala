package bad.robot.temperature.client

import java.lang.Runtime.getRuntime
import java.util.concurrent._

import bad.robot.temperature.task.TemperatureMachineThreadFactory
import org.http4s.client.blaze.SimpleHttp1Client

import scala.concurrent.duration._

object BlazeHttpClient {

  private val DefaultTimeout: Duration = 5.minutes
  private val DefaultExecutor = {
    new ThreadPoolExecutor(1, getRuntime.availableProcessors() * 6, 60L, SECONDS, new LinkedBlockingQueue[Runnable](), TemperatureMachineThreadFactory("client"))
  }

  def apply() = SimpleHttp1Client(DefaultTimeout, executor = DefaultExecutor, sslContext = None)
}
