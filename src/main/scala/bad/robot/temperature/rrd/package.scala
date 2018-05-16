package bad.robot.temperature

import org.rrd4j.core.{RrdDb, Sample}

import scala.Double._
import scala.concurrent.duration.Duration

package object rrd {

  val anHour = Duration(1, "hour")
  val aDay = Duration(24, "hours")
  val aWeek = Duration(7, "days")
  val aMonth = Duration(30, "days")
  
  implicit class RrdDbOps(database: RrdDb) {
    def hasValuesFor(datasource: String): Boolean = {
      DataSources.updated.contains(database.getDatasource(database.getDsIndex(datasource)).getName)
    }
  }

  implicit class RrdSampleOps(sample: Sample) {
    def setValues(database: RrdDb, time: Seconds, values: Double*) = {
      val matched = (0 until values.size)
        .dropRight(values.size - database.getDsCount)
        .filter(index => !(values(index) equals NaN))
        .map(index => database.getDatasource(index).getName)

      sample.setTime(time)
      sample.setValues(values: _*)
      sample.update()

      DataSources.updated = DataSources.updated ++ matched.toSet
    }
  }

  object DataSources {
    var updated = Set[String]()
  }
}
