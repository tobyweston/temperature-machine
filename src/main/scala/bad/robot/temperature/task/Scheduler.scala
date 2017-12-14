package bad.robot.temperature.task

import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import bad.robot.logging._

import scala.concurrent.duration.Duration

object Scheduler {

  implicit class ScheduledExecutorServiceOps(executor: ScheduledExecutorService) {
    def schedule(frequency: Duration, tasks: Runnable*): List[ScheduledFuture[_]] = {
      this.schedule(frequency, printError(_), tasks:_*)
    }

    def schedule(frequency: Duration, errorHandler: Throwable => Runnable => Unit, tasks: Runnable*): List[ScheduledFuture[_]] = {
      tasks.map(task => {
        executor.scheduleAtFixedRate(wrapWithErrorHandler(task, errorHandler), 0, frequency.length, frequency.unit)
      }).toList
    }
  }

  def wrapWithErrorHandler(task: Runnable, errorHandler: Throwable => Runnable => Unit): Runnable = {
    () => try {
      task.run()
    } catch {
      case e: Throwable => errorHandler(e)(task)
    }
  }

  private def printError(e: Throwable): Runnable => Unit = {
    task => Log.error(s"An error occurred executed a scheduled task ($task) ${e.getMessage}")
  }
}