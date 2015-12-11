@echo off
@echo Starting ReadClock...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% ReadClock %1 %2

