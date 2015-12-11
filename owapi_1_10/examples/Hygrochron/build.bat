@echo off
@echo Compiling...
javac -deprecation -classpath ..\..\lib\OneWireAPI.jar;%classpath% -d . .\src\*.java

