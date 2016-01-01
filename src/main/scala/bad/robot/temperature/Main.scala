package bad.robot.temperature

import java.io.File

import bad.robot.temperature.TemperatureMachineThreadFactory._
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Rrd, RrdFile}
import bad.robot.temperature.server.Server

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  SensorFile.find().headOption match {
    case Some(file) => start(file)
    case _          => println(FailedToFindFile(BaseFolder).message)
  }

  private def start(file: File) = {
    print("RRD initialising...")
    RrdFile(30 seconds).create()

    print("Starting monitoring...")
    Scheduler(30 seconds, createThreadPool("reading-thread")).start(Measurement(SensorReader(file), Rrd()))
    Scheduler(90 seconds, createThreadPool("graphing-thread")).start(GenerateGraph(Duration(24, "hours")))
    Scheduler(100 seconds, createThreadPool("xml-export-thread")).start(XmlExport(Duration(24, "hours")))

    print("Server initialising...")
    Server(11900).start()
  }
}
