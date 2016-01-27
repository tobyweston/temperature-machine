package bad.robot.temperature.task

import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import bad.robot.temperature.task.Scheduler._
import org.http4s.util.task

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scalaz.concurrent.Task

object Scheduler {
  def apply(frequency: Duration, executor: ScheduledExecutorService) = new Scheduler(frequency, executor)

  implicit class ScheduledExecutorServiceOps(executor: ScheduledExecutorService) {
    def schedule(frequency: Duration, tasks: Runnable*) = {
      tasks.map(task => executor.scheduleWithFixedDelay(wrapWithErrorHandler(task), 0, frequency.length, frequency.unit)).toList
    }
  }

  def wrapWithErrorHandler(task: Runnable): Runnable = {
    new Runnable {
      def run() = try {
        task.run()
      } catch {
        case e: Throwable => System.err.println(s"An error occurred executed a scheduled task ($task) ${e.getMessage}")
      }
    }
  }

}

class Scheduler(frequency: Duration, executor: ScheduledExecutorService) {

  def start(tasks: Runnable*): List[ScheduledFuture[_]] = {
    tasks.map(task => executor.scheduleWithFixedDelay(wrapWithErrorHandler(task), 0, frequency.length, frequency.unit)).toList
  }

  def task(tasks: Runnable*): Task[List[ScheduledFuture[_]]] = Task.delay {
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

}
