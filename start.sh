#!/usr/bin/env bash

TEMPERATURE_HOME=~/.temperature
LOG_FILE=${TEMPERATURE_HOME}/temperature-machine.log
IP=$( ifconfig wlan0 | grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}' )

mkdir ${TEMPERATURE_HOME} -p

nohup java -Xmx512m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=${IP} -jar target/scala-2.12/temperature-machine-2.1.jar > ${LOG_FILE} 2>&1 &

ln -s -F ${LOG_FILE} temperature-machine.log

echo "$!" > temperature-machine.pid
echo "Started your temperature-machine (server mode), monitoring default RRD;"
echo "   Redirecting output to ${LOG_FILE}"
echo "   PID stored in temperature-machine.pid"
