@echo off
@echo Compiling...
javac -classpath ..\..\lib\OneWireAPI.jar;%classpath% -d . .\src\*.java

