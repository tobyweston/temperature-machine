#!/usr/bin/env bash

args=("$@")

TEMPERATURE_HOME=~/.temperature
LOG_FILE=${TEMPERATURE_HOME}/temperature-machine.log

ETH0=`grep "eth0" /proc/net/dev`
if  [ -n "$ETH0" ] ; then
   LAN="eth0"
else
   LAN="wlan0"
fi
IP=$( ip -f inet addr show ${LAN} | grep -Po 'inet \K[\d.]+' )

mkdir ${TEMPERATURE_HOME} -p

nohup java -Dcom.sun.management.jmxremote=true -Dcom.sun.management.jmxremote.port=1616 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=${IP} -cp target/scala-2.12/temperature-machine-2.2.jar bad.robot.temperature.Main $@ > ${LOG_FILE} 2>&1 &

rm -f temperature-machine.log
ln -s -F ${LOG_FILE} temperature-machine.log

echo "$!" > temperature-machine.pid
echo "Started your temperature-machine (server-mode), monitoring $@;"
echo "   Redirecting output to ${LOG_FILE}"
echo "   PID stored in temperature-machine.pid"
echo "   JMX enabled, discovered ${LAN} on ${IP}"