@echo off
@echo Starting ReadDigPot...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% ReadDigPot %1 %2

