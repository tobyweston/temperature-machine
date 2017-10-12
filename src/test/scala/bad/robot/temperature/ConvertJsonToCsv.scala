package bad.robot.temperature

import java.io._
import java.net.URL
import java.time.Instant
import java.time.ZoneId._
import java.time.format.DateTimeFormatter._
import java.time.format.FormatStyle._
import java.util.Locale._

import argonaut._

import scala.io.Source

object ConvertJsonToCsv extends App {

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
    case Left(error)  => println(error)
    case Right(series) => toCsv(series)
  }
  
  def toCsv(series: List[Series]) = {
    val quote = "\""
    val formatter = ofLocalizedDateTime(SHORT).withLocale(UK).withZone(systemDefault())
    
    def toRows: List[(String, Instant, Double)] = for {
      reading     <- series
      measurement <- reading.data 
    } yield {
      (reading.label, measurement.time, measurement.temperature.celsius)
    }

    def toCsv(rows: List[(String, Instant, Double)]): List[String] = {
      val enquote: String => String = value => s"$quote$value$quote"
      val heading = List(
        enquote("Sensor"), 
        enquote("Time"), 
        enquote("Temperature")
      ).mkString(",")
      
      heading :: rows.map(row => List(
        enquote(row._1),
        enquote(formatter.format(row._2)),
        enquote(row._3.toString)
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

