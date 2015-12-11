@echo off
@echo Starting initMission...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% initMission %1 %2

