#!/usr/bin/env bash

PID_FILE=temperature-machine.pid

if [ -f ${PID_FILE} ]; then
    PID=$(cat ${PID_FILE})
    echo "Stopping temperature-machine on PID $PID"
    kill ${PID} || true
    rm ${PID_FILE}
else
    echo "No temperature-machine running"
fi