---
layout: docs
title: Introduction
---

# Introduction

The temperature-machine is a low cost, do-it-yourself data logger built around the Raspberry Pi and the [DS18D20](https://datasheets.maximintegrated.com/en/ds/DS18B20.pdf) temperature sensor. You can track ambient temperature over days, weeks and months and display some pretty graphs.

## Distributed Design

The temperature-machine can run on one or more Raspberry Pis connected to one or more temperature sensors. One Pi acts as the _hub_ or _server_ whilst additional machines act as _clients_ and upload their temperature data to the hub. The hub itself should be connected to a temperature sensor.

Each Pi can have up to five physical sensors attached. This is so that you can get an average temperature for the room (say, placing one sensor high in the room and another at floor level) or up to five individual temperatures. All temperatures will be graphed.

By convention, each machine will identify itself by it's hostname. So, if you have a machine in the office, change the hostname (via `raspi-config`) to `office`.

The server broadcasts it's address over the network. When clients start up, they wait to hear the broadcast and perform a basic handshake. The server's broadcast message includes the servers address. So when a client hears it, it knows where to send it's temperature data.


### Example Setup

The following uses three physical Pi Zeros setup in the following configuration. Each has the room name set as the machine's `hostname`.

 * `study` has the _server_ with one sensor
 * `bedroom 1` has a _client_ with two sensors
 * `kitchen` has a _client_ with one sensor
 
<img src="../img/temperature-machine-alt.png" alt="" width="667" height="517" style="max-width:100%;">




## Hardware

Build it yourself with the following.

| Item | Price |
|------|-------|
| [Raspberry Pi Zero W](https://shop.pimoroni.com/products/raspberry-pi-zero-w) | £ 10
| [SanDisk 8GB microSDHC memory card](http://amzn.to/1T6zIc9) | £ 4
| [2.54mm Header strip](http://amzn.to/1pIKZ7m)  | £ 0.89
| [DS18B20 1-Wire temperature sensor](http://amzn.to/1RhmOHc)    | £ 1.45
| 1 x 4.7k Ω resistor | £ 0.10
| [Some jumper wires](http://amzn.to/1Rlrbj9) or otherwise recycled wires with connectors |    £ 0.97
| Software (you're looking at it) | <span style="color:green;">[FREE](https://github.com/tobyweston/temperature-machine)</span>
| | &nbsp;
| **Total** | **£ 16.44**

**Optional extras** You could go for a regular [Pi Zero](https://shop.pimoroni.com/products/raspberry-pi-zero) (£ 4) and [USB Wifi adapter](http://amzn.to/1RhmTKQ) (about £ 6), a case (I like the one from [Switched On Components](https://socomponents.co.uk/shop/black-laser-cut-acrylic-raspberry-pi-zero-case-v2/) at £ 3.80) and a USB to TTL serial connection for headless setup. Something with a PL2302TA chip in it like [this module](http://amzn.to/1ZtRWoA) or the [Adafruit console cable](https://www.adafruit.com/product/954). See my post on using a [Pi Console Lead](http://baddotrobot.com/blog/2015/12/28/pi-console-lead/).