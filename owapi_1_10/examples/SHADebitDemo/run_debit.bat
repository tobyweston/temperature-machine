@echo off
@echo Starting SHADebitDemo...
java -classpath ..\..\lib\OneWireAPI.jar;%classpath% SHADebitDemo %1 %2

