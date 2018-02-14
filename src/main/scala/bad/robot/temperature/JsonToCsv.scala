package bad.robot.temperature

import java.lang.Math.abs
import java.time.Instant
import java.time.ZoneId._
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle._
import java.util.Locale

import io.circe.Decoder

import scalaz.\/

object JsonToCsv {

  type Row = (String, Instant, Double)
 
  val DefaultTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(SHORT).withLocale(Locale.getDefault).withZone(systemDefault())
  
  def convert(json: => Error \/ String, formatter: DateTimeFormatter): Error \/ String = {
    for {
      string <- json
      series <- decodeAsDisjunction[List[Series]](string)
    } yield {
      toCsv(series, formatter)
    }
  }
  
  private def toCsv(series: List[Series], formatter: DateTimeFormatter) = {
    val quote = "\""
    
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
        enquote("Difference")
      ).mkString(",")
      
      if (rows.isEmpty)
        return Nil
      
      val tuples = rows.map(x => (x._1, x._2, x._3, "0"))
      val rowsWithPercentageDifference = tuples.drop(1).scan(tuples.head) {
        case (previous, current) => (current._1, current._2, current._3, f"${abs(current._3 - previous._3)}%.2f")
      }
      
      heading :: rowsWithPercentageDifference.map(row => List(
        enquote(row._1),
        enquote(formatter.format(row._2)),
        enquote(row._3.toString),
        enquote(row._4.toString)
      ).mkString(","))
    }
    
    toCsv(toRows).mkString(sys.props("line.separator"))
  }
}

object Series {
  import io.circe.generic.semiauto._

  implicit val dataCodec: Decoder[Series] = deriveDecoder[Series]
}
case class Series(label: String, data: List[Data])

object Data {
  import io.circe.generic.semiauto._

  implicit val seriesCodec: Decoder[Data] = deriveDecoder[Data]
}
case class Data(x: Long, y: String) {
  def time: Instant = Instant.ofEpochMilli(x)
  def temperature: Temperature = Temperature(y.toDouble)
}
