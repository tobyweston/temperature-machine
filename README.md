
## 1-Wire

The `w1-therm` module will output the sensor readings (the contents of the 1-Wire "scratchpad") to a file. For example,

    a3 01 4b 46 7f ff 0e 10 d8 : crc=d8 YES
    a3 01 4b 46 7f ff 0e 10 d8 t=32768

On the sensor itself, the measurements are stored in an area of memory called the "scratchpad". It's addressed using the following byte table.

Position | Description
--- | ---
Byte 0 | Temperature (least significant byte)
Byte 1 | Temperature (most significant byte))
Byte 2 | Th register on user byte 1
Byte 3 | Tl register on user byte 1
Byte 4 | Configuration register
Byte 5 | Reserved
Byte 6 | Reserved
Byte 7 | Reserved
Byte 8 | CRC (e.g. `d8` in the example above)


The file format has some additional information as it pulls out the result of the CRC check (`YES`/`NO`) and does the temperature calculation for you. If you're interested, you can calculate the temperature from the bytes yourself using the following formula (where `data` is the array of bytes from the above and `0.0625` is a conversion coefficient between the sensor internal values and physical temperature).

    ((data[1] << 8) + data[0]) *0.0625

or



https://tushev.org/images/electronics/arduino/ds18x20/scratchpad.PNG
https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf
https://tushev.org/articles/arduino/10