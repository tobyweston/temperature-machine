#!/usr/bin/env bash

IP=$( ifconfig wlan0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}' )
nohup java -Xmx512m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=$IP -jar target/scala-2.11/temperature-machine-1.0.jar > temperature-machine.log 2>&1 &

echo "$!" > temperature-machine.pid

echo "Started your temperature-machine, redirecting output to temperature-machine.log, PID stored in temperature-machine.pid"
