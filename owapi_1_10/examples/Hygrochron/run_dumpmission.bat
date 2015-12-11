@echo off
@echo Starting dumpmission...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% dumpmission %*

