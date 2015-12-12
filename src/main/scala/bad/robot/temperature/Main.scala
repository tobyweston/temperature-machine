package bad.robot.temperature

import java.util.concurrent.Executors._
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration

object Main extends App {

  private val threadPool = newScheduledThreadPool(1, new ThreadFactory() {
    private val threadCount = new AtomicInteger
    def newThread(runnable: Runnable): Thread = new Thread(runnable, "temperature-reading-thread-" + threadCount.incrementAndGet())
  })

  Scheduler(Duration(5, "seconds"), threadPool).start(TemperatureProbe())

}
