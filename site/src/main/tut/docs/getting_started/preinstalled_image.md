---
layout: docs
title: Download Pre-installed Image
---

# Download Pre-installed Image

I followed the previous steps to [Build from Source](build_from_source.html) then ran `sudo dd if=/dev/rdisk3 of=2017-03-02-raspbian-temperature-machine.img bs=1m` to create an image file based Raspian Jessie Lite (4.9.y). 

1. Download the image from my [Google Drive](https://drive.google.com/open?id=0B-I9xnCr64hFWjBoS0Z6akUwVVU).
1. On Mac, use [Etcher](https://etcher.io/) to flash a new SD card with the image. Refer to [raspberrypi.org](https://www.raspberrypi.org/documentation/installation/installing-images/) for other platforms.
1. Setup your wifi by inserting your new SD card to your machine and creating a file `wpa_supplicant.conf` under `/boot`. Mine looks like this (the `scan_ssid=1` is only needed if you're using a hidden network). 

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
    1. If this is the first temperature-machine, run in _server_ mode. Add the following to `/etc/rc.local` above the `exit 0` line.
    
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


## Extras

The image comes with a few extras including;
 
* Disabled activity light (edit `/boot/config.txt` to disable)
* Disabled LED for Edimax wifi adapters (disable under `/lib/modules/$(uname -r)/kernel/drivers/net/wireless/realtek/rtl8192cu/`)
* temperature-machine source code under `~/code/temperature-machine`
* the Linux source under `~/linux`, useful for compiling patched drivers
