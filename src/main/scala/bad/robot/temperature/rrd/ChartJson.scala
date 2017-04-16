package bad.robot.temperature.rrd

import argonaut.Argonaut._
import argonaut._
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
      series.zip(temperatures).map { case (label, temperature) => (time, label, temperature) }
    }

    val bySeries: PartialFunction[(Time, Sensor, Celsius), Sensor] = {
      case (_, sensor, _) => sensor
    }

    val toSeries: PartialFunction[(Sensor, Seq[(Time, Sensor, Celsius)]), Series] = {
      case series => Series(series._1, series._2.map { case (time, _, celsius) => Point(time, celsius) }.toList)
    }

    measurements
      .groupBy(bySeries)
      .map(toSeries)
      .toList
      .sortBy(_.name)
  }
}

case class Series(name: Sensor, data: List[Point])

object Series {

  implicit def seriesEncoder: EncodeJson[Series] = {
    EncodeJson((series: Series) =>
      Json(
        "label" := jString(series.name),
        "data"  := series.data
      )
    )
  }

  implicit def seriesDecoder: DecodeJson[Series] = {
    DecodeJson(cursor => for {
      label <- cursor.get[String]("label")
      data  <- cursor.get[List[Point]]("data")
    } yield Series(label, data))
  }

}

case class Point(time: String, celsius: String)

object Point {
  implicit def tupleToPoint(tuple: (String, String)): Point = Point(tuple._1, tuple._2)

  implicit def seriesEncoder: EncodeJson[Point] = {
    EncodeJson((point: Point) =>
      Json(
        "x" -> jNumber(Seconds.secondsToDuration(Seconds(point.time.toLong)).toMillis),
        "y" -> jString(point.celsius)
      )
    )
  }

  implicit def seriesDecoder: DecodeJson[Point] = {
    DecodeJson(cursor => for {
      x <- cursor.get[Long]("x")
      y <- cursor.get[String]("y")
    } yield Point(x.toString, y))
  }
}