package bad.robot.temperature

import bad.robot.temperature.TemperatureMachineThreadFactory._
import bad.robot.temperature.rrd.RrdFile
import bad.robot.temperature.server.Server

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  RrdFile(30 seconds).create()

  Scheduler(30 seconds, createThreadPool("reading-thread")).start(Measurements.sensorToRrd())
  Scheduler(90 seconds, createThreadPool("graphing-thread")).start(GenerateGraph(Duration(24, "hours")))
  Scheduler(100 seconds, createThreadPool("xml-export-thread")).start(XmlExport(Duration(24, "hours")))

  Server(11900).start()

}
