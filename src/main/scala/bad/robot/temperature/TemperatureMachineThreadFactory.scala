package bad.robot.temperature

import java.lang.Thread.UncaughtExceptionHandler
import java.util.concurrent.Executors._
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object TemperatureMachineThreadFactory {
  private val threadCount = new AtomicInteger

  def createThreadPool(name: String) = newScheduledThreadPool(1, new ThreadFactory() {
    def newThread(runnable: Runnable): Thread = new Thread(runnable, s"temperature-$name-" + threadCount.incrementAndGet())
  })

}
