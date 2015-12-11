@echo off
@echo Compiling.  Make sure MinML is installed (http://www.minml.com)
javac -classpath ..\..\lib\OneWireAPI.jar;%classpath% -d . .\src\*.java

