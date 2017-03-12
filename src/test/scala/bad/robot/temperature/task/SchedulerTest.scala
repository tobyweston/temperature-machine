package bad.robot.temperature.task

import java.net.{Socket => _}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicInteger

import bad.robot.temperature.task.Scheduler.ScheduledExecutorServiceOps
import org.specs2.mutable.Specification

import scala.concurrent.duration._

class SchedulerTest extends Specification {

  "Exceptions aren't propagated when wrapped" >> {
    val handler = Scheduler.wrapWithErrorHandler(() => throw new Exception())
    handler.run must not(throwA[Exception])
  }

  "Executes at fixed rate" >> {
    val scheduler = new ScheduledExecutorServiceOps(Executors.newSingleThreadScheduledExecutor())
    val counter = new AtomicInteger(0)
    scheduler.schedule(1 milliseconds, () => counter.getAndIncrement())

    counter.get() must be_>(2).eventually
  }

  "Executes at fixed rate without stopping when exceptions are thrown" >> {
    val scheduler = new ScheduledExecutorServiceOps(Executors.newSingleThreadScheduledExecutor())
    val counter = new AtomicInteger(0)
    scheduler.schedule(1 milliseconds, () => {
      counter.getAndIncrement()
      throw new Exception()
    })

    counter.get() must be_>(2).eventually
  }

}
