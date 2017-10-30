#!/usr/bin/env bash

TEMPERATURE_HOME=~/.temperature
LOG_FILE=${TEMPERATURE_HOME}/temperature-machine.log
IP=$( ip -f inet addr show wlan0 | grep -Po 'inet \K[\d.]+' )

mkdir ${TEMPERATURE_HOME} -p

nohup java -Xmx512m -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=${IP} -cp target/scala-2.12/temperature-machine-2.1.jar bad.robot.temperature.client.Client > ${LOG_FILE} 2>&1 &

rm -f temperature-machine.log
ln -s -F ${LOG_FILE} temperature-machine.log

echo "$!" > temperature-machine.pid
echo "Started your temperature-machine (client-mode);"
echo "   Redirecting output to ${LOG_FILE}"
echo "   PID stored in temperature-machine.pid"
