---
layout: docs
title: Installing
---

# Installing

1. Setup 1-wire Support by adding `dtoverlay=w1-gpio` to [`/boot/config.txt`](https://www.raspberrypi.org/documentation/configuration/config-txt/README.md). <span class="bg-danger">You can skip this step if it's already present.</span>
    
    ```
    sudo bash -c 'echo "dtoverlay=w1-gpio" >> /boot/config.txt'
    ```
    
    Reboot to take effect.

1. Setup `apt-get` to recognise the temperature-machine repository. To ensure the releases come from official, trusted sources, also add the repository key using `apt-key`.

    ```
    sudo bash -c 'echo "deb http://robotooling.com/debian stable temperature-machine" >> /etc/apt/sources.list'
    sudo apt-key adv --keyserver pool.sks-keyservers.net --recv-keys 00258F48226612AE
    ```

1. If your `/etc/apt/sources.list` file contains the following line, remove it. It was used by a previous version.

    ```
    deb http://robotooling.com/debian ./"
    ```

1. If you've previously built from source, stop any running versions (not required for a fresh install).

    ```
    cd ~/code/temperature-machine   
    ./stop.sh
    ```

    You might also want to disable any previous automatic startup (if you built from source). Check the contents of `/etc/rc.local` and remove the following line if it exists (you might see `start-server.sh` instead of `start-client.sh`).
    
    ```
    su pi -c 'cd /home/pi/code/temperature-machine && ./start-client.sh &'
    ```

1. Install. 

    ```
    sudo apt-get update
    sudo apt-get install temperature-machine
    ```

1. Decide if you will be running the temperature-machine as a **server** or **client**.

    If you have a single machine, you want a **server**. If you already have a server running and want to monitor more rooms with more machines, add each of these as clients.

    Run the following before you startup the temperature-machine for the first time.
    
    ```
    temperature-machine --init
    ```
    
    It will ask you to choose and create the configuration file as `~/.temperature/temperature-machine.cfg`.

1. If you created a server configuration file above, update the default hosts.

    The default configuration will list some default values for `hosts`, such as:

    ```
    hosts = ["garage", "lounge", "study"]
    ```

    These are the machines you will be using in your final setup. <span class="bg-warning">Ensure these match the host names of each machine you plan to add</span>. The values are used to initialise the RRD database on the server. If you need to change this later, you will have to delete the `~/.temperature/temperature.rrd` file, losing historic data. It's worth adding in some spares.

    <p class="bg-warning">
    After setting up the configuration, you can either restart the service manually (run <code>sudo systemctl restart temperature-machine</code>) or wait about a minute and it will restart automatically.
    </p>
    
    If you had previously built from source and modified your `/etc/rc.local` referencing the `start-server.sh` script. You can use the values here to populate your configuration. It would have includes the list of hosts after the `start-server.sh` command. 

1. Check the logs for activity (to check it's all running ok)

    ```
    cat ~/.temperature/temperature-machine.log
    ```

1. Try it out. Go to [http://your_hostname:11900]() from your favorite browser.


## Updating

Run the `apt-get update` command to update your Raspberry Pi's package lists with the latest versions of software, then upgrade temperature-machine:

    sudo apt-get update
    sudo apt-get install --only-upgrade temperature-machine
    
    

## Miscellaneous

### Running as a Service

By default, the temperature-machine runs as a service in the background - it should be running as soon as you've installed it with `apt-get`. It will start on boot and restart if it develops a problem. We use [`systemd`](https://en.wikipedia.org/wiki/Systemd) to achieve this. 

You can list all the services with `systemctl list-unit-files` and verify temperature-machine is `enabled`. If it's disabled, run `sudo systemctl enable temperature-machine` to enable (and start) it.

Check the current status at any time with `systemctl status temperature-machine`.

If you want to disable it running as a service, run `sudo systemctl disable temperature-machine` and take a look at the [section below](#running-manually) to start manually.

If you want to stop the service temporally, use `sudo systemctl stop temperature-machine`.

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

