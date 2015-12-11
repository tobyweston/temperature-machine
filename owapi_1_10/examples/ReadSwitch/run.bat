@echo off
@echo Starting ReadSwitch...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% ReadSwitch %1 %2

