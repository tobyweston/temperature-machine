---
layout: docs
title: Build From Source
---

# Build From Source ![](https://travis-ci.org/tobyweston/temperature-machine.svg?branch=master)

## Prerequisites

You'll need Java installed on your Pi (`sudo apt-get install oracle-java8-jdk`), Git (`sudo apt-get install git`) and SBT. 

### Install SBT

To setup `sbt` manually, follow these steps from the terminal.

    cd /usr/local/bin
    sudo wget https://repo.typesafe.com/typesafe/ivy-releases/org.scala-sbt/sbt-launch/0.13.16/sbt-launch.jar
    sudo chown pi sbt-launch.jar
    sudo chgrp pi sbt-launch.jar

Create a file `/usr/local/bin/sbt` (change the owner and group as above) and paste the following in (take note that the max memory is set to 512 MB for the Pi Zero). Change the owner and group as above.

    #!/bin/bash
    SBT_OPTS="-Xms512M -Xmx512M"
    java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"
    
Then make it executable.

    chmod u+x /usr/local/bin/sbt


Or if you prefer the official install, these steps (from [scala-sbt.org](http://www.scala-sbt.org/0.13/docs/Installing-sbt-on-Linux.html)).

    echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    sudo apt-get install dirmngr --install-recommends
    sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
    sudo apt-get update
    sudo apt-get install sbt


## Building 

The general approach to get the software on the box, I tend to do the following.

1. Clone the Git repository on the Pi
1. (Recommended) increase your swap file size (see [below](build_from_source.html#increase-swap-file-size))
1. Run `sbt -J-Xmx512m -J-Xms512m assembly` from a terminal (memory set low for the Pi Zero). This might take around 30m on the Pi Zero.
1. Run `./start.sh &` from the checked out folder

> Don't forget to enable 1-wire support by adding `dtoverlay=w1-gpio` to `/boot/config.txt`

You can also read my [blog post](http://baddotrobot.com/blog/2016/03/23/homebrew-temperature-logger/) for more detailed instructions and the [configuration section](../configuration.html) to find out how to start automatically on boot. 

## Keep up to Date 

Keep your source up to date by getting the latest from Git (`git pull`) and compiling with `sbt assembly`.

1. `cd ~/code/temperature-machine`
1. `git pull`
1. `sbt -J-Xmx512m -J-Xms512m assembly`

This is the "in-development" version of the software. It may have partially implemented features or be broken completely. Check that the build is at least passing (below) before you update and if you prefer, stick to [downloading](download.html) the [official releases](https://github.com/tobyweston/temperature-machine/releases) as an alternative.


## Tips and Troubleshooting

### Always run as `pi`

All the steps assume you're running as the `pi` user. **<mark>Never run anything to do with temperature-machine as root</mark>**.

### Increase Swap File Size

SBT seems to struggle on the Pi and `assembly` will frequently fail. It uses incremental compilation so running a second time after a failure picks up from where it left off and will generally succeed.

However, if you increase the swap size on the Pi (especially effective on the Pi Zero), you should have fewer problems.

Edit `/etc/dphys-swapfile` to increase the swap file size:


    $ sudo nano /etc/dphys-swapfile

Replace:

    CONF_SWAPSIZE=100

with...

    CONF_SWAPSIZE=512

Recreate the swap file with the following.

    sudo /etc/init.d/dphys-swapfile restart

Check by running `free -m`. The total value for swap should be `512` (ish) (and not `99`).

    $ free -m
                  total        used        free      shared  buff/cache   available
    Mem:            434          98         174           3         161         285
    Swap:           511           0          511

Thanks to Cristian Arezzini for this tip.


### Failing Build

An SBT build should end with a success message. Something like this.

    ...
    [warn] Strategy 'discard' was applied to 11 files
    [info] SHA-1: 5f0a3316b670b1249256892d76322b0ddec9e47a
    [info] Packaging /home/pi/code/temperature-machine/target/scala-2.12/temperature-machine-2.1.jar ...
    [info] Done packaging.
    [success] Total time: 1688 s, completed 18-Jan-2018 19:29:18
  
Anything else is a failure. 

Failures will be either environmental (problems with the Pi like memory or general flakeyness) or problems with the code (doesn't compile or tests fail). If it's environmental, follow the steps above: rerun and/or increase the swap file size. Here's an example of a environmental failure during compilation.

    ...
    Compiling 29 Scala sources to /home/pi/code/temperature-machine/target/scala-2.12/test-classes...
    /usr/local/bin/sbt: line 3:  6014 Killed                  java $SBT_OPTS -jar `dirname $0`/sbt-launch.jar "$@"


If it's a compilation or test failure, you'll need to reach out (see the [Getting Help](../getting_help.html) section).


### Skipping Tests

If you want to skip running the tests as part of the assembly process, use the following command:

    sbt 'set test in assembly := {}' assembly 
    
    
### Starting and Stopping

If you build from source, you'll have to start and stop the application manually. 

Use the following commands respectively.

    ./start.sh
    ./stop.sh
    
 
### Starting Automatically

There are lots of different ways to start software automatically after a reboot. You might choose to add following to `/etc/rc.local` (for a longer term solution, follow the [installation](installing.html) instructions and run as a service). 

    su pi -c 'cd /home/pi/code/temperature-machine && ./start.sh &'

It uses the configuration file (see [Installing](getting_started/installing.html) section) to select between server and clients so ensure the configuration file exists in the `~/.temperature` folder.

The command runs the start script as the user `pi` and assumes you’ve cloned the code as to `/home/pi/code/temperature-machine`. After a reboot, it will execute and you should see a log file in `~/.temperature` and pid file in the startup folder.

To stop, just run the `stop.sh` script.

    