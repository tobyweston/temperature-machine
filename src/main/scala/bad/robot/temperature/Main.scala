package bad.robot.temperature

import java.util.concurrent.Executors._
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import bad.robot.temperature.rrd.RrdFile

import scala.concurrent.duration.Duration

object Main extends App {

  private val threadPool = newScheduledThreadPool(1, new ThreadFactory() {
    private val threadCount = new AtomicInteger
    def newThread(runnable: Runnable): Thread = new Thread(runnable, "temperature-reading-thread-" + threadCount.incrementAndGet())
  })

  val frequency = Duration(30, "seconds")
  RrdFile(frequency).create()
  Scheduler(frequency, threadPool).start(Measurements.sensorToRrd())

}
