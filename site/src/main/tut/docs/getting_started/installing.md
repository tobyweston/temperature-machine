---
layout: docs
title: Installing
---

# Installing

The general approach is as follows.

1. Setup 1-wire support
1. Setup `apt-get` with my Debian repository
1. Install via `apt-get`
1. Create a configuration file before the first run
1. Run from the terminal or as a service / daemon

The following steps should be run from the terminal.


## Setup 1-wire Support

<p class="bg-danger">
You can skip this step if <code>dtoverlay=w1-gpio</code> is already present in <code>config.txt</code>.
</p>

You will need to enable 1-wire support by adding `dtoverlay=w1-gpio` to [`/boot/config.txt`](https://www.raspberrypi.org/documentation/configuration/config-txt/README.md).
   
    sudo bash -c 'echo "dtoverlay=w1-gpio" >> /boot/config.txt'

Reboot for this to take effect.


## Setup `apt-get`

Do this only once to get `apt-get` to recognise the temperature-machine repository.

    sudo bash -c 'echo "deb http://robotooling.com/debian ./" >> /etc/apt/sources.list'


## Install 

    sudo apt-get update
    sudo apt-get install temperature-machine



## Running for the First Time

Decide if you will be running the temperature-machine as a **server** or **client**.

If you have a single machine, you want a **server**. If you already have a server running and want to monitor more rooms, add each of these as clients.

Run the following before you startup the temperature-machine for the first time.

    temperature-machine --init

It will create the configuration file as `~/.temperature/temperature-machine.cfg` after prompting and encode your decision.


### Server Configuration

If you created a server configuration file, it will list some default values for `hosts`. For example:

    hosts = ["garage", "lounge", "study"]

These are the machines you will be using in your final setup. <span class="bg-warning">Ensure these match the host names of each machine you plan to add</span>. The values are used to initialise the RRD database on the server. If you need to change this later, you will have to delete the `~/.temperature/temperature.rrd` file, losing historic data. It's worth adding in some spares.


## Running

You can run with the following.

    temperature-machine
    
If you want to check the version you're running (a good way to check it's installed), try the following.

    temperature-machine -- -v
    
You can also run `man temperature-machine` to read about more options.

<p class="bg-warning">
The log file can be found in <code>~/.temperature/temperature-machine.log</code>. The conventional <code>`/var/log/temperature-machine`</code> is currently unused.
</p>


## Running as a Service

We use [`systemd`](https://en.wikipedia.org/wiki/Systemd) as our service wrapper / init system. You can list all the services with `systemctl list-unit-files`.
 
If you see temperature-machine is enabled, there's nothing else to do: it will start on boot.

If it's disabled, run `systemctl enable temperature-machine`.

You can find out more about the current status with `systemctl status temperature-machine`.


## Updating

You have to run the `update` command to recognise new versions, then upgrade temperature-machine with the following.

    sudo apt-get update
    sudo apt-get install --only-upgrade temperature-machine
    
    