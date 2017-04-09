package bad.robot.temperature.rrd

import bad.robot.temperature.rrd.ChartJson._

import scala.collection.immutable.Seq
import scala.xml.Elem

object ChartJson {

  type Time = String
  type Sensor = String
  type Celsius = String

  def parse(rdd: Elem): List[Series] = {
    val series = (rdd \\ "datasources" \ "name").map(_.text)
    val rows = rdd \\ "data" \ "row"
    val data = rows.map { row =>
      val time = row \ "timestamp"
      val values = row \ "values" \ "v"
      (time.text, values.map(_.text))
    }

    val measurements = data.flatMap { case (time, temperatures) =>
      series.zip(temperatures).map(x => (time, x._1, x._2))
    }

    val bySeries: PartialFunction[(Time, Sensor, Celsius), Sensor] = {
      case (_, sensor, _) => sensor
    }

    val toSeries: PartialFunction[(Sensor, Seq[(Time, Sensor, Celsius)]), Series] = {
      case series => Series(series._1, series._2.map { case (time, _, celsius) => (time, celsius) }.toList)
    }

    measurements
      .groupBy(bySeries)
      .map(toSeries)
      .toList
      .sortBy(_.name)
  }
}

case class Series(name: Sensor, data: List[(Time, Celsius)])

object Series {

  import argonaut._
  import Argonaut._

  implicit def seriesEncoder: EncodeJson[Series] = {
    EncodeJson((series: Series) =>
      Json(
        "label" -> jString(series.name),
        "data" -> data(series.data)
      )
    )
  }

  private def data(measurements: List[(Time, Celsius)]): Json = {
    Json.array(measurements.map(point): _*)
  }

  private def point(measurement: (Time, Celsius)): Json = {
    Json(
      "x" -> jNumber(Seconds.secondsToDuration(Seconds(measurement._1.toLong)).toMillis),
      "y" -> jString(measurement._2)
    )
  }

}
