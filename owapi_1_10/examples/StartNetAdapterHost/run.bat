@echo off
@echo Instructions: The home directory of your OWAPI distribution must
@echo               be set to the environment variable OWAPI_HOME
@echo                  For example:  set OWAPI_HOME=e:\owapi
@echo Starting StartNetAdapterHost...
java -classpath ..\..\lib\OneWireAPI.jar;. StartNetAdapterHost %1 %2 %3 %4 %5 %6 %7 %8 %9

