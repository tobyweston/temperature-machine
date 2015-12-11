@echo off
@echo Starting OWWatch...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% OWWatch %1

