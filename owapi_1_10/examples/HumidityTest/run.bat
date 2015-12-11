@echo off
@echo Instructions: The home directory of your OWAPI distribution must  
@echo               be set to the environment variable OWAPI_HOME  
@echo                  For example:  set OWAPI_HOME=e:\owapi
@echo Starting HumidityTest...
java -classpath ..\..\lib\OneWireAPI.jar;. HumidityTest 

