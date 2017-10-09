---
layout: docs
title: Getting Started
---

# Getting Started

## Setup the Hardware

Connecting the temperature sensor to the Pi is straight forward. There a bunch of [tutorials on the web](https://www.google.co.uk/search?btnG=1&pws=0&q=pi+ds18b20+tutorial) but you're looking to connect the following physical pins on the Pi to the following sensor connectors.

Physical Pi Pin | Description | DS18b20 Connector
----------------|-------------|--------
1 | 3.3v Power  | Power (<span style="color:red;">red</span>)
7 | GPIO 4      | Data (<span style="color:orange;">yellow</span>)
9 | Ground      | Ground (<span style="color:black;">black</span>)

The other thing you'll need to do is connect the 4.7k Ω resistor between the power and data lines. This acts as a [pull-up resistor](https://learn.sparkfun.com/tutorials/pull-up-resistors) to ensure that the Pi knows that the data line starts in a "high" state. Without it, it can't tell if it should start as high or low; it would be left _floating_.

Soldering a resistor inline can be a bit tricky, so I build a add-on board to make the whole process simpler. See the [Add-on Board](add-on_board.html) section for details.

## Setup the Pi

Make sure you have the following line in your `/boot/config.txt`. It will load the GPIO [1-wire](https://pinout.xyz/pinout/1_wire) driver and any attached temperature sensor should be automatically detected.

    dtoverlay=w1-gpio


Older tutorials on the web will also say you have to load the `w1-therm` module but that seems to load automatically these days.


## Setup the Software

In terms of deployment (running the temperature-machine software), you have three options here.

1. Build and run from source
1. Download a pre-installed Raspian image with the above pre-done
1. Download the binary distribution (`temperature-machine-2.1.jar`) and run manually

