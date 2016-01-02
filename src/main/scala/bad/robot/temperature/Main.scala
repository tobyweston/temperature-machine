package bad.robot.temperature

import java.io.File
import java.net.InetAddress

import bad.robot.temperature.TemperatureMachineThreadFactory._
import bad.robot.temperature.ds18b20.SensorFile._
import bad.robot.temperature.ds18b20.{SensorFile, SensorReader}
import bad.robot.temperature.rrd.{Rrd, RrdFile}
import bad.robot.temperature.server.Server

import scala.concurrent.duration._
import scala.language.postfixOps

object Main extends App {

  val Port = 11900

  SensorFile.find("src/test/resources/examples").headOption match {
    case Some(file) => start(file)
    case _          => println(FailedToFindFile(BaseFolder).message)
  }

  private def start(file: File) = {
    print("RRD initialising...")
    if (!RrdFile.exists) RrdFile(30 seconds).create() else println("Ok")

    print("Starting monitoring...")
    Scheduler(30 seconds, createThreadPool("reading-thread")).start(Measurement(SensorReader(file), Rrd()))
    Scheduler(90 seconds, createThreadPool("graphing-thread")).start(GenerateGraph(Duration(24, "hours")))
    Scheduler(100 seconds, createThreadPool("xml-export-thread")).start(XmlExport(Duration(24, "hours")))
    println("Ok")

    println(s"Server started on http://${InetAddress.getLocalHost.getHostAddress}:$Port")
    Server(Port).start()
  }
}
