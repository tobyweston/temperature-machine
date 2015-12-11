@echo off
@echo Starting startmission...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% startmission %*

