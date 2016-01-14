package bad.robot.temperature

import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

object Scheduler {
  def apply(frequency: Duration, executor: ScheduledExecutorService) = new Scheduler(frequency, executor)
}

class Scheduler(frequency: Duration, executor: ScheduledExecutorService) {

  def start(tasks: Runnable*): List[ScheduledFuture[_]] = {
    tasks.map(task => executor.scheduleWithFixedDelay(wrapWithErrorHandler(task), 0, frequency.length, frequency.unit)).toList
  }

  def cancel(tasks: List[ScheduledFuture[_]]) {
    tasks.foreach(task => task.cancel(true))
  }

  def stop: List[Runnable] = {
    val tasks = executor.shutdownNow.asScala
    tasks.foreach(task => task.asInstanceOf[ScheduledFuture[_]].cancel(true))
    tasks.toList
  }

  def wrapWithErrorHandler(task: Runnable): Runnable = {
    new Runnable {
      def run() = try {
        print(".")
        task.run()
      } catch {
        case e: Throwable => System.err.println(e.getMessage)
      }
    }
  }

}
