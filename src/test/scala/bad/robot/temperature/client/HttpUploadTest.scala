package bad.robot.temperature.client

import org.specs2.mutable.Specification

class HttpUploadTest extends Specification {

  "Ip address" >> {
    HttpUpload.currentIpAddress.size must be_>(0)
  }
}
