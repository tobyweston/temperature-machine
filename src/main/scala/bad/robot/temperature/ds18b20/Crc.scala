package bad.robot.temperature.ds18b20

object CrcLookupTable {

  private val table: Array[Byte] = new Array[Byte](256)

  for (i <- 0 to 255) {
    var acc = i
    var crc = 0
    for (j <- 0 to 7) {
      crc = if (((acc ^ crc) & 0x01) == 0x01) ((crc ^ 0x18) >> 1) | 0x80 else crc >> 1
      acc = acc >> 1
    }
    table(i) = crc.toByte
  }

  def lookup(index: Int) = table(index)

}

/** CRC8 is based on the polynomial = X^8 + X^5 + X^4 + 1. */
object Crc {

  def compute(data: Int, seed: Int): Int = CrcLookupTable.lookup((seed ^ data) & 0x0FF) & 0x0FF

  def compute(data: Int): Int = CrcLookupTable.lookup(data & 0x0FF) & 0x0FF

  def compute(data: Array[Byte]): Int = compute(data, 0, data.length)

  def compute(data: Array[Byte], offset: Int, length: Int): Int = compute(data, offset, length, 0)

  def compute(data: Array[Byte], offset: Int, length: Int, seed: Int): Int = {
    var crc = seed
    for (byte <- 0 until length) {
      crc = CrcLookupTable.lookup((crc ^ data(byte + offset)) & 0x0FF)
    }
    crc & 0x0FF
  }

  def compute(data: Array[Byte], seed: Int): Int = compute(data, 0, data.length, seed)
}