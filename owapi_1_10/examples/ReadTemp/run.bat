@echo off
@echo Starting ReadTemp...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% ReadTemp %1 %2

