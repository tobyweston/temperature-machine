#!/usr/bin/env bash
# sbt assembly
scp target/scala-2.12/temperature-machine-2.1.jar pi@study.local:/home/pi/code/temperature-machine/target/scala-2.12
scp target/scala-2.12/temperature-machine-2.1.jar pi@kitchen.local:/home/pi/code/temperature-machine/target/scala-2.12
scp target/scala-2.12/temperature-machine-2.1.jar pi@bedroom1.local:/home/pi/code/temperature-machine/target/scala-2.12
scp target/scala-2.12/temperature-machine-2.1.jar pi@bedroom2.local:/home/pi/code/temperature-machine/target/scala-2.12