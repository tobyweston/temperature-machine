package bad.robot.temperature.rrd

import bad.robot.temperature.rrd.ChartJson._
import io.circe._

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
      series.zip(temperatures).map { case (label, temperature) => (time, label, temperature) }
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

  implicit def seriesEncoder: Encoder[Series] = new Encoder[Series] {
    def apply(series: Series): Json = Json.obj(
      ("label", Json.fromString(series.name)),
      ("data" -> data(series.data))
    )
  }

  private def data(measurements: List[(Time, Celsius)]): Json = {
    Json.arr(measurements.map(point): _*)
  }

  private def point(measurement: (Time, Celsius)): Json = {
    Json.obj(
      "x" -> Json.fromLong(Seconds.secondsToDuration(Seconds(measurement._1.toLong)).toMillis),
      "y" -> Json.fromString(measurement._2)
    )
  }

}
