package bad.robot.temperature

import org.specs2.mutable.Specification

class IpAddressTest extends Specification {

  "Valid IP addresses" >> {
    IpAddress.isIpAddress("10.0.1.7") must_== true
    IpAddress.isIpAddress("0.0.0.0") must_== true
    IpAddress.isIpAddress("255.255.255.255") must_== true
  }

  "Invalid IP addresses" >> {
    IpAddress.isIpAddress("fe80:0:0:0:40c3:9f41:82e7:760d%utun1") must_== false
    IpAddress.isIpAddress("256.255.255.255") must_== false
    IpAddress.isIpAddress("255.256.255.255") must_== false
    IpAddress.isIpAddress("255.255.256.255") must_== false
    IpAddress.isIpAddress("256.255.255.256") must_== false
    IpAddress.isIpAddress("0.0.0.-1") must_== false
  }

  "Encode Json" >> {
    encode(IpAddress("10.0.1.7")).spaces2ps must_==
      """|{
         |  "value" : "10.0.1.7"
         |}""".stripMargin
  }
}
