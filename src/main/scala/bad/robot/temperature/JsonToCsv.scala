package bad.robot.temperature

import java.time.Instant
import java.time.ZoneId._
import java.time.format.DateTimeFormatter._
import java.time.format.FormatStyle._
import java.util.Locale

import argonaut._
import bad.robot.temperature.PercentageDifference.percentageDifference

import scalaz.\/
import scalaz.syntax.std.either._

object JsonToCsv {

  type Row = (String, Instant, Double)
 
  def convert(json: => Error \/ String): Error \/ String = {
    for {
      string <- json
      series <- Parse.decodeEither[List[Series]](string).disjunction.leftMap(ParseError)
    } yield {
      toCsv(series)
    }
  }
  
  private def toCsv(series: List[Series]) = {
    val quote = "\""
    val formatter = ofLocalizedDateTime(SHORT).withLocale(Locale.getDefault).withZone(systemDefault())
    
    def toRows: List[Row] = for {
      reading     <- series
      measurement <- reading.data 
    } yield {
      (reading.label, measurement.time, measurement.temperature.celsius)
    }

    def toCsv(rows: List[Row]): List[String] = {
      val enquote: String => String = value => s"$quote$value$quote"
      val heading = List(
        enquote("Sensor"), 
        enquote("Time"), 
        enquote("Temperature"),
        enquote("%Difference")
      ).mkString(",")
      
      if (rows.isEmpty)
        return Nil
      
      val tuples = rows.map(x => (x._1, x._2, x._3, 0D))
      val rowsWithPercentageDifference = tuples.drop(1).scan(tuples.head) {
        case (previous, current) => (current._1, current._2, current._3, percentageDifference(previous._3, current._3)) 
      }
      
      heading :: rowsWithPercentageDifference.map(row => List(
        enquote(row._1),
        enquote(formatter.format(row._2)),
        enquote(row._3.toString),
        enquote(row._4.toString)
      ).mkString(","))
    }
    
    toCsv(toRows).mkString("\n")
  }
}

object Series {
  implicit val dataCodec: CodecJson[Series] = CodecJson.derive[Series]
}
case class Series(label: String, data: List[Data])

object Data {
  implicit val seriesCodec: CodecJson[Data] = CodecJson.derive[Data]
}
case class Data(x: Long, y: String) {
  def time: Instant = Instant.ofEpochMilli(x)
  def temperature: Temperature = Temperature(y.toDouble)
}
