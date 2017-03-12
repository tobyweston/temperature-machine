package bad.robot.temperature.task

import java.util.concurrent.{ScheduledExecutorService, ScheduledFuture}

import scala.concurrent.duration.Duration

object Scheduler {

  implicit class ScheduledExecutorServiceOps(executor: ScheduledExecutorService) {
    def schedule(frequency: Duration, tasks: Runnable*): List[ScheduledFuture[_]] = {
      tasks.map(task => executor.scheduleAtFixedRate(wrapWithErrorHandler(task), 0, frequency.length, frequency.unit)).toList
    }
  }

  def wrapWithErrorHandler(task: Runnable): Runnable = {
    () => try {
      task.run()
    } catch {
      case e: Throwable => System.err.println(s"An error occurred executed a scheduled task ($task) ${e.getMessage}")
    }
  }

}