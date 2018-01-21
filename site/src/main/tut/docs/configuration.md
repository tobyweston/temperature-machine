---
layout: docs
title: Configuration
---

# Configuration

See the [release notes](https://github.com/tobyweston/temperature-machine/releases) as not all features listed here may be available on your version.

## The Startup Scripts

There are currently two startup scripts.

1. `start-server.sh`
1. `start-client.sh`

You can run these manually from the terminal or setup the Pi to run then automatically on boot (see below). To stop, run the `stop.sh` script.

### Start Server

Run the following to startup the server ready to monitor three machines. Always include the host name of the machine you're running (it requires the server is monitoring temperatures).

    ./start-server.sh machine1 machine2 machine3

### Start Client

Just run the following to start the client. You must have a server running on your network. If you have just one machine, run just the server.

    ./start-client.sh


## Start Automatically

There are different ways to start software automatically after a reboot. I chose to add the following to `/etc/rc.local` on the server. Replace `garage` and `bedroom` with the names of the machines you want to monitor (the default name of a new Raspberry Pi install will be `raspberrypi` but you might want to change each host name to reflect the room the Pi sits in).

    su pi -c 'cd /home/pi/code/temperature-machine && ./start-server.sh garage bedroom &'

...and the following to the client(s).

    su pi -c 'cd /home/pi/code/temperature-machine && ./start-client.sh &'

They run the startup scripts as the user `pi` and assumes you’ve cloned the code as to `/home/pi/code/temperature-machine`. After rebooting, you should see a log file in `~/.temperature` and pid file in the startup folder.

To stop, just run the `stop.sh` script.


## Avoid Spikes in Charts

<span class="label label-success">version 2.1 only</span>

[Issue #9](https://github.com/tobyweston/temperature-machine/issues/9) tries to address occasional spikes in temperature data by tracking previous temperatures and ignoring fluctuations. If enabled, ignore temperatures with a +/-25% fluctuation by default. Enable and configure by adding the following to your server startup script (`30` is an example percentage).

    -Davoid.spikes=30 
    
So, edit `/home/pi/code/temperature-machine/start-server.sh` and change the following line.

    nohup java -Xmx512m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=${IP} -cp target/scala-2.12/temperature-machine-2.1.jar bad.robot.temperature.server.Server $@ > ${LOG_FILE} 2>&1 &

to

<pre class="highlight"><code class="hljs bash">nohup java <mark>-Davoid.spikes=30</mark> -Xmx512m -Dcom.sun.management.jmxremote=<span class="hljs-literal">true</span> -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=<span class="hljs-literal">false</span> -Dcom.sun.management.jmxremote.ssl=<span class="hljs-literal">false</span> -Djava.rmi.server.hostname=<span class="hljs-variable">${IP}</span> -cp target/scala-2.12/temperature-machine-2.1.jar bad.robot.temperature.server.Server <span class="hljs-variable">$@</span> &gt; <span class="hljs-variable">${LOG_FILE}</span> 2&gt;&amp;1 &amp;
</code></pre>    

You can verify it's working by checking the log on the server machine. It should look something like the following.

<pre class="highlight"><code class="hljs bash">Sun 21-Jan-201819:48:18.384 [main] INFO Starting temperature-machine (server mode)...
Sun 21-Jan-201819:48:18.573 [main] INFO RRD initialising for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge' (with up to 5 sensors each)...
Sun 21-Jan-201819:48:18.651 [main] INFO Ok
Sun 21-Jan-201819:48:24.453 [main] INFO Starting Discovery Server, listening for 'study', 'bedroom1', 'bedroom2', 'bedroom3', 'outside', 'kitchen', 'lounge'...
Sun 21-Jan-201819:48:24.559 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...
Sun 21-Jan-201819:48:24.954 [main] INFO Monitoring sensor file(s) on 'study' 
	/sys/bus/w1/devices/28-031591c760ff/w1_slave

Sun 21-Jan-201819:48:25.156 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...
Sun 21-Jan-201819:48:25.163 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...
<mark>Sun 21-Jan-201819:48:27.401 [main] INFO Temperature spikes greater than +/-30% will not be recorded</mark>
Sun 21-Jan-201819:48:42.148 [main] INFO HTTP Server started on http://127.0.1.1:11900
Sun 21-Jan-201819:48:42.547 [temperature-machine-discovery-server-1] INFO Listening for broadcast messages...
</code></pre>