---
layout: docs
title: Raspbian Quick Start Guide
---

# Raspbian Quick Start Guide

Follow these steps to get up and running with a fresh install of Raspbian Lite, all ready for the temperature-machine.

1. Download the image of [Raspbian Stretch Lite](https://www.raspberrypi.org/downloads/raspbian/)
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
1. Whilst you're there, add `dtoverlay=w1-gpio` to the `/boot/config.txt` file.
1. Unmount and stick the card in the Pi. Power up. 
1. On the Pi (`ssh pi@raspberrypi.local`), run `raspi-config` to:
    1. Set the hostname to the room name (`garage`, `study` etc) 
    1. Expand the file system
1. Reboot. Follow the [install steps](installing.html).


## Extras

You may want some optional extras:
 
* Disable the activity light (edit `/boot/config.txt` to disable)
* Disable LED for Edimax wifi adapters (disable under `/lib/modules/$(uname -r)/kernel/drivers/net/wireless/realtek/rtl8192cu/`)
