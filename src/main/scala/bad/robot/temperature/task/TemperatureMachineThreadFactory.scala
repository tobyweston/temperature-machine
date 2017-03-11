package bad.robot.temperature.task

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object TemperatureMachineThreadFactory {
  def apply(name: String, daemon: Boolean = false): ThreadFactory = {
    new ThreadFactory() {
      val count = new AtomicInteger
      def newThread(runnable: Runnable): Thread = {
        val thread = new Thread(runnable, s"temperature-$name-" + count.incrementAndGet())
        thread.setDaemon(daemon)
        thread
      }
    }
  }
}