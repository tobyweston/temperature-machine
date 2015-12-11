@echo Instructions: The home directory of your TINI distribution must be set to
@echo               the environment variable TINI_HOME
@echo               For example:  set TINI_HOME=e:\tini1.02
@echo off
mkdir tini
@echo Compile...
javac -bootclasspath %TINI_HOME%\bin\tiniclasses.jar -classpath ..\..\lib\owapi_dependencies_TINI.jar;. -d tini src\*.java
@echo TINI Convert...
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\SHADebitDemo.class -x ..\..\lib\owapi_dep.txt -o tini\SHADebitDemo.tini -add SHADebit -d %TINI_HOME%\bin\tini.db
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\initcopr.class -x ..\..\lib\owapi_dep.txt -o tini\initcopr.tini -add SHADebit -d %TINI_HOME%\bin\tini.db
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\initrov.class -x ..\..\lib\owapi_dep.txt -o tini\initrov.tini -add SHADebit -d %TINI_HOME%\bin\tini.db
java -classpath %TINI_HOME%\bin\tini.jar;. BuildDependency -p ..\..\lib\owapi_dependencies_TINI.jar -f tini\initrov33.class -x ..\..\lib\owapi_dep.txt -o tini\initrov33.tini -add SHADebit -d %TINI_HOME%\bin\tini.db

