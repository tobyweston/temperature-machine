package bad.robot.temperature

import argonaut.Parse
import bad.robot.temperature.rrd.Series

import scala.io.Source

object JsonToCsv extends App {

  val bedroom = "/Users/toby/Workspace/github/temperature-machine/src/test/resources/examples/bedroom1-sensor-1.json"
  val kitchen = "/Users/toby/Workspace/github/temperature-machine/src/test/resources/examples/kitchen-sensor-1.json"
  val study = "/Users/toby/Workspace/github/temperature-machine/src/test/resources/examples/study-sensor-1.json"
  // 1492020060000 -> (19.25) 3775.9375

  val json = Source.fromFile(kitchen).getLines.mkString("\n")

  Parse.decodeEither[Series](json) match {
    case Right(series) =>
      var last = 0.0
      for (plot <- series.data) {
        if (plot.celsius != "NaN") {
          val celsius = plot.celsius.toDouble
          if (celsius - last > 5 || last - celsius > 5)
            println(s"${plot.time} -> ($last) $celsius")
          last = celsius
        }
      }
    case Left(e) =>
      println("error " + e)
  }

//  val   s = Parse.decodeWithEither[String, Series](json, x => {
//    println("ok  " + x.name)
//    "ok"
//  }, {
//    case Left(msg)             => "got an error parsing: " + msg
//    case Right((msg, history)) => "got an error decoding: " + msg + " - " + history
//  })
  println("end")
}