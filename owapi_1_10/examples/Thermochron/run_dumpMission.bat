@echo off
@echo Starting dumpMission...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% dumpMission %1 %2

