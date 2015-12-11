@echo Instructions: The home directory of your TINI distribution must be set to 
@echo               the environment variable TINI_HOME  
@echo               For example:  set TINI_HOME=e:\tini1.02
@echo off
mkdir tini
@echo Compile...
javac -bootclasspath %TINI_HOME%\bin\tiniclasses.jar -classpath ..\..\lib\owapi_dependencies_TINI.jar;. -d tini src\*.java
@echo TINI Convert...
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\dumpMission.class -x ..\..\lib\owapi_dep.txt -o tini\dumpMission.tini -add OneWireContainer21 -d %TINI_HOME%\bin\tini.db
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\initMission.class -x ..\..\lib\owapi_dep.txt -o tini\initMission.tini -add OneWireContainer21 -d %TINI_HOME%\bin\tini.db



