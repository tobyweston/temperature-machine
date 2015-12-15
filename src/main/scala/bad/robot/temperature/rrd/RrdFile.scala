package bad.robot.temperature.rrd

import bad.robot.temperature.rrd.RrdTemperature._
import org.rrd4j.ConsolFun._
import org.rrd4j.DsType.GAUGE
import org.rrd4j.core._

import scala.Double._
import scala.concurrent.duration.Duration


object RrdFile {
  val path = "temperature.rrd"
}

case class RrdFile(frequency: Seconds = Duration(30, "seconds")) {

  def create(start: Seconds = now()) = {

    def numberOfStepsFor(duration: Duration) = (duration.toSeconds / frequency).toInt

    def numberOfRowsFor(period: Duration, numberOfSteps: Int): Int = {
      val result = (period.toSeconds / (frequency.value * numberOfSteps)).toInt
      println(s"rows for $frequency frequency in $period : $result")
      result
    }

    val heartbeat = frequency + Seconds(5)

    val definition = new RrdDef(RrdFile.path, start, frequency)
    val noAggregation = numberOfStepsFor(frequency)
    val hourlyAggregation = numberOfStepsFor(anHour)
    val daily = new ArcDef(AVERAGE, 0.5, noAggregation, numberOfRowsFor(aDay, noAggregation))
    val dailyHourAvg  = new ArcDef(AVERAGE, 0.5, hourlyAggregation, numberOfRowsFor(aDay, hourlyAggregation))
//    val weekly = new ArcDef(AVERAGE, 0.5, hourlyAggregation, numberOfRowsFor(aWeek, hourlyAggregation))

    val temperature = new DsDef(name, GAUGE, heartbeat, NaN, NaN)

    definition.addDatasource(temperature)
    definition.addArchive(daily)
    definition.addArchive(dailyHourAvg)
//    definition.addArchive(weekly)

    createFile(definition)
  }

  private def createFile(definition: RrdDef) = {
    val database = new RrdDb(definition)
    database.close()
    println("File created; " + definition.dump())
  }

}