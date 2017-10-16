package bad.robot.temperature.client

import org.specs2.mutable.Specification

class HttpUploadTest extends Specification {

  "Ip address" >> {
//    println(HttpUpload.currentIpAddress.map(_.map(_.getHostAddress)).mkString("\n"))
    HttpUpload.currentIpAddress.size must be_>(0)
  }
}
