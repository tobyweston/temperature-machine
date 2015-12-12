
## 1-Wire

The `w1-therm` module will output the sensor readings (the contents of the 1-Wire "scratchpad") to a file. For example,

    4b 01 4b 46 7f ff 05 10 d8 : crc=d8 YES
    4b 01 4b 46 7f ff 05 10 d8 t=20687

On the sensor itself, the measurements are stored in an area of memory called the "scratchpad". It's addressed using the following byte table.

Position | Description
--- | ---
Byte 0 | Temperature (least significant byte)
Byte 1 | Temperature (most significant byte)
Byte 2 | TH register on user byte 1 (alarm high)
Byte 3 | TL register on user byte 2 (alarm low)
Byte 4 | Configuration register
Byte 5 | Reserved
Byte 6 | Reserved
Byte 7 | Reserved
Byte 8 | CRC (e.g. `d8` in the example above)


The file format has some additional information as it pulls out the result of the CRC check (`YES`/`NO`) and does the temperature calculation for you. If you're interested, you can calculate the temperature from the bytes yourself by rearranging the first two bytes with least significant at the front;

    01 4b

then treating it as a single hex value, divide by `16`.

    scala> 0x014b / 16.0
    res: Double = 20.6875


### References

[Interesting discussion](https://www.raspberrypi.org/forums/viewtopic.php?f=37&t=91982)
[DS18B20 Datasheet](https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf)

