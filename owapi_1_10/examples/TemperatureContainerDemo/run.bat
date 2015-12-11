@echo off
@echo Starting TemperatureContainerDemo...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% TemperatureContainerDemo %1 %2

