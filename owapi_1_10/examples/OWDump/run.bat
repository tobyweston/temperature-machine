@echo off
@echo Starting OWDump...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% OWDump %1 %2

