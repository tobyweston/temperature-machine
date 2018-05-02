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
1. Restart the service


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



## Before you Run

Decide if you will be running the temperature-machine as a **server** or **client**.

If you have a single machine, you want a **server**. If you already have a server running and want to monitor more rooms with more machines, add each of these as clients.

Run the following before you startup the temperature-machine for the first time.

    temperature-machine --init

It will create the configuration file as `~/.temperature/temperature-machine.cfg` after prompting and encode your decision.


### Server Configuration

If you created a server configuration file, it will list some default values for `hosts`. For example:

    hosts = ["garage", "lounge", "study"]

These are the machines you will be using in your final setup. <span class="bg-warning">Ensure these match the host names of each machine you plan to add</span>. The values are used to initialise the RRD database on the server. If you need to change this later, you will have to delete the `~/.temperature/temperature.rrd` file, losing historic data. It's worth adding in some spares.

<p class="bg-warning">
After setting up the configuration, you can either restart the service manually (run <code>sudo systemctl restart temperature-machine</code>) or wait for it to restart itself.
</p>


## Running

### Running as a Service

By default, the temperature-machine runs as a service in the background - it should be running as soon as you've installed it with `apt-get`. It will start on boot and restart if it develops a problem. We use [`systemd`](https://en.wikipedia.org/wiki/Systemd) to achieve this. 

You can list all the services with `systemctl list-unit-files` and verify temperature-machine is `enabled`. If it's disabled, run `sudo systemctl enable temperature-machine` to enable (and start) it.

Check the current status at any time with `systemctl status temperature-machine`.

If you want to disable it running as a service, run `sudo systemctl disable temperature-machine` and take a look at the [section below](#running-manually) to start manually.

<p class="bg-warning">
If there is a problem, the service will attempt to restart after 60 seconds. That means, if it can't start up (for example, becuase there are no sensors attached), it will keep trying until the problem is resolved.
</p>


### Running Manually

Assuming it isn't running already (stop it with `sudo systemctl stop temperature-machine`), you can start it manually with the following (stopping with <kbd>ctrl + c</kbd>).

    temperature-machine
    
If you want to check the version you're running (a good way to check it's installed), try the following.

    temperature-machine -- -v
    
You can also run `man temperature-machine` to read about more options.

<p class="bg-warning">
The log file can be found in <code>~/.temperature/temperature-machine.log</code>. The conventional <code>/var/log/temperature-machine</code> is currently unused.
</p>


## Updating

Run the `apt-get update` command to update your Raspberry Pi's package lists with the latest versions of software, then upgrade temperature-machine:

    sudo apt-get update
    sudo apt-get install --only-upgrade temperature-machine
    
    