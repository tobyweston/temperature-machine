
# Temperature Machine 

![](https://travis-ci.org/tobyweston/temperature-machine.svg?branch=master)

A temperature-machine for the Raspberry Pi. 

![](temperature-machine.png)

## Quick Start (in an IDE)

If you want to play with the source, you can build some test data by running the `Example` app from within IntelliJ.

You can then start up the app from the `Main` class. If you want to override the sensor file location (for the case when you're testing without sensors), use `-Dsensor-location=src/test/resources/examples`.

Check the web page with [http://localhost:11900](http://localhost:11900).


## Deploying to Your Pi

If you want to run it in earnest, deploy to your Pi.

On the Pi, you should only need to add the following line `/boot/config.txt` to enable [1-wire](https://pinout.xyz/pinout/1_wire). Older tutorials on the web will also say you have to load the `w1-therm` module but that seems to load automatically these days.

    dtoverlay=w1-gpio


In terms of deployment (running the temperature-machine software), you have three options here.

1. Build and run from source
1. Download a pre-installed Raspian image with the above pre-done
1. Download the binary distribution (`temperature-machine-2.0.jar`) and run manually

### Build from Source

To get the software on the box, I tend to do the following;

1. Clone the repository on the Pi
2. Run `sbt -J-Xmx512m -J-Xms512m assembly` from a terminal (memory set low for the Pi Zero). This might take around 30m on the Pi Zero.
3. Run `./start.sh &`, `./start-server.sh room1 room2 room3` or `./start-client.sh` from the checked out folder

You can also read my [blog post](http://baddotrobot.com/blog/2016/03/23/homebrew-temperature-logger/) for more detailed instructions (including automatically logging temperatures on reboot). Don't forget to `git pull` the latest version every so often.

### Download a Pre-installed Raspian Image

I followed the previous steps to [Build from Source](#Build_From_Source) then ran `sudo dd if=/dev/rdisk3 of=2017-03-02-raspbian-temperature-machine.img bs=1m count=2000` to create an image file based Raspian Jessie Lite (the `count=2000` was to reduce a 16GB card down to 2GB having messed about with partions with [GParted](http://gparted.org/index.php) on a Linux box first).

1. Download the image from my [Google Drive](https://drive.google.com/open?id=0B-I9xnCr64hFczR4aTBsRFNuZlU).
1. On Mac, use [Etcher](https://etcher.io/) to flash a new SD card with the image. Refer to [raspberrypi.org](https://www.raspberrypi.org/documentation/installation/installing-images/) for other platforms.
1. Setup you're wifi by inserting your new SD card to your machine and creating a file `wpa_supplicant.conf` under `/boot`. Mine looks like this (the `scan_ssid=1` is only needed if you're using a hidden network). 

    ```
    ctrl_interface=DIR=/var/run/wpa_supplicant GROUP=netdev
    update_config=1
    
    network={
      ssid="ssid"
      scan_ssid=1
      psk="plain text password"
    }
    ```
1. Unmount and stick the card in the Pi. Power up. 
1. On the Pi (`ssh pi@raspberrypi.local`), run `raspi-config` to:
    1. Set the hostname to the room name (`garage`, `study` etc) 
    1. Expand the file system
1. Setup the Pi to start temperature-machine on boot
    1. If this is the first temperature-machine, run in server mode. Add the following to `/etc/rc.local` above the `exit 0` line.
    
        ```
        su pi -c 'cd /home/pi/code/temperature-machine && ./start-server.sh garage bedroom &'
        ```
    
    Set `garage` and `bedroom` to be the host names of all the temperature-machines you intend to run. Don't forget to include the name of the server node (this machine).
    
    1. If you already have a server, run in client mode. Add the following to `/etc/rc.local` above the `exit 0` line.

        ```
        su pi -c 'cd /home/pi/code/temperature-machine && ./start-client.sh &'
        ```
    
1. Reboot. Enjoy.

The source has been cloned to `~/code/temperature-machine`. To keep up to date with changes, go into that folder and run `git pull` followed by `sbt assembly` occasionally.


### Download Binary Release

If you already have Java and SBT running. You can run download the binary from the [bad.robot.repo](http://robotooling.com/maven/) repository. Just run the following to start the server up.

    java -jar temperature-machine-2.0.jar bad.robot.temperature.server.Server garage bedroom
    
or

    java -jar temperature-machine-2.0.jar bad.robot.temperature.client.Client

to start up the client.
    

## Client / Server

The server broadcasts it's address over the network. The client(s) will wait to hear the broadcast then startup, sending their data to the server.


## RRD and Cleanup

Because we have to declare what's in the RRD upfront, you'll have to delete the `~/.temperature/temperature.rdd` if the configuration changes. Starting the server for the first time will initialise the RRD; it will create "archives" for every machine it's expecting to monitor. These are passed in as command line arguments (`room1`, `room2` and `room3` in the above example).

So adding an additional machine may mean you need to do `rm ~/.temperature/*`

Starting the server up when the `~/temperature.rrd` already exists **doesn't** delete it.


## DS18B20 Sensor & 1-Wire

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

## Misc Setup

Make sure any Wifi dongle doesn't fall asleep; famously the Edimax EW-7811UN does. Create a config file for the wifi module with `sudo nano /etc/modprobe.d/8192cu.conf` and add this line

    options 8192cu rtw_power_mgnt=0 rtw_enusbss=0


Check it's running with `lsmod`, you should see something like this

    $ lsmod
    Module                  Size  Used by
    8192cu                569532  0


Setup a `rc.local` file to boot to the thing as the `pi` user;

    $ cat /etc/rc.local
    #!/bin/sh -e
    su pi -c 'cd /home/pi/code/temperature-machine && ./start.sh &'

    exit 0


Connecting to a non-hidden network is straight forward. Setting things up for a hidden network is [a little more involved](http://www.dafinga.net/2013/01/how-to-setup-raspberry-pi-with-hidden.html).


## Contributing

See the [Developer's Readme](DEVELOPERS.md)


## Pi Stats

Load on the Pi is pretty minimal, however the XML export and graph generation do max out the CPU for short periods.

![](cpu_memory.png)


## References

* [Interesting discussion on the sensor](https://www.raspberrypi.org/forums/viewtopic.php?f=37&t=91982)
* [DS18B20 Datasheet](https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf)
* [Disable power management for the Edimax wifi dongle](https://www.raspberrypi.org/forums/viewtopic.php?t=61665)


