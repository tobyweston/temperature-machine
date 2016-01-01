#!/usr/bin/env bash

nohup java -server -Xmx512m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -jar target/scala-2.11/temperature-machine-1.0.jar > temperature-machine.log 2>&1 &
echo "$!" > temperature-machine.pid

echo "Started your temperature-machine, redirecting output to temperature-machine.out, PID stored in temperature-machine.pid"
