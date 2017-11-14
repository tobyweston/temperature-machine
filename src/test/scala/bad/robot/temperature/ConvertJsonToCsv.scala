package bad.robot.temperature

import java.io._
import java.net.URL
import java.time.Instant
import java.time.ZoneId._
import java.time.format.DateTimeFormatter._
import java.time.format.FormatStyle._
import java.util.Locale._

import argonaut._
import bad.robot.temperature.PercentageDifference.percentageDifference

import scala.io.Source

object ConvertJsonToCsv extends App {

  type Row = (String, Instant, Double)
  
  case class Series(label: String, data: List[Data])
  case class Data(x: Long, y: String) {
    def time: Instant = Instant.ofEpochMilli(x)
    def temperature: Temperature = Temperature(y.toDouble)
  }

  val exampleJson =
    """
      |[
      |  {
      |    "label": "bedroom1-sensor-1",
      |    "data": [
      |      {
      |        "x": 1507709610000,
      |        "y": "NaN"
      |      },
      |      {
      |        "x": 1507709640000,
      |        "y": "+2.2062500000E01"
      |      },
      |      {
      |        "x": 1507709680000,
      |        "y": "+2.2262500000E01"
      |      }
      |    ]
      |  }
      |]
    """.stripMargin
  
  val serverAddress = "study.local"
  
  val json = Source.fromURL(new URL(s"http://$serverAddress:11900/temperature.json")).getLines().mkString

  implicit val seriesCodec: CodecJson[Data] = CodecJson.derive[Data]
  implicit val dataCodec: CodecJson[Series] = CodecJson.derive[Series]
  
  Parse.decodeEither[List[Series]](json) match {
    case Left(error)   => Log.error(error)
    case Right(series) => toCsv(series)
  }
  
  def toCsv(series: List[Series]) = {
    val quote = "\""
    val formatter = ofLocalizedDateTime(SHORT).withLocale(UK).withZone(systemDefault())
    
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
    
    def write(rows: List[String]) = {
      val writer = new PrintWriter(new File("temperatures.csv"))
      writer.write(rows.mkString("\n"))
      writer.close()
    }

    write(toCsv(toRows))
  }
}

