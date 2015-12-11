@echo off
@echo Starting OWNetWatch...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% OWNetWatch %1

