---
layout: docs
title: DS18B20 Sensor
---

# DS18B20 Sensor & 1-Wire

The `w1-therm` module will output the sensor readings to a file. For example,

    4b 01 4b 46 7f ff 05 10 d8 : crc=d8 YES
    4b 01 4b 46 7f ff 05 10 d8 t=20687



This file is derived from an area of the sensor's memory called the "scratchpad". The scratchpad is addressed using the following byte table.

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


This is how the tempreature-machine software goes about it.


### Sensor Precision

Byte 4 of the scratchpad is a configuration byte, you can use it to set the conversion resolution; the number of decimal places the temperature will show as. The DS18B20 is supposed to be set at the most accurate (12 bit) at the factory but I've had units that were set to record temperatures at 0.5 °C steps (9 bit).

Byte 4 Hex | Precision | Example
--- | --- | ---
1F | 9 bit | 23.5
3F | 10 bit | 23.45
5F | 11 bit | 23.445
7F | 12 bit | 23.4445


So if you see something like this in your scratchpad.

    59 01 4b 01 1f ff 0c 10 39 : crc=39 YES
    59 01 4b 01 1f ff 0c 10 39 t=23500

the `1f` shows the sensor is set to 9 bit resolution.

If your sensor shipped with low resolution you can change it straight from the Pi using `configDS18B20.c` from [Dan Perron](https://github.com/danjperron/BitBangingDS18B20).

    $ curl https://raw.githubusercontent.com/danjperron/BitBangingDS18B20/master/configDS18B20.c -O
    $ gcc -lrt -o configDS18B20 configDS18B20.c
    $ sudo ./configDS18B20

It'll output something like the following and prompt you to enter a new resolution.

    GPIO 4
    BCM GPIO BASE= 20000000
    ....78 01 4B 46 1F FF 0C 10 FA
    09 bits  Temperature:  23.50 +/- 0.500000 Celsius
    DS18B20 Resolution (9,10,11 or 12) ?12
    Try to set 12 bits  config=7F

Now the scratchpad shows the updated value (`7f` below)

    $ cat /sys/bus/w1/devices/28-0115910f5eff/w1_slave
    75 01 4b 46 7f ff 7f 10 f5 : crc=f5 YES
    75 01 4b 46 7f ff 7f 10 f5 t=23312

