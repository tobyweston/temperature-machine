Readme.txt

-----------------------------------------
| DS1922/DS2422 Hygrochron Demo         |
-----------------------------------------

I. Description
=------------=

DS1922 temperature and A-D/Humidity gathering demo consisting of two
applications. One application 'missions' a DS1922/DS2422 to collect
data and the other 'dumps' the mission data.

II. Files
=-------=
readme.txt             - this file
run_dumpmission.bat    - batch file to run dumpmission
run_startmission.bat   - batch file to run startmission
build.bat              - batch file to rebuild this example
tini_build.bat         - batch file to rebuild this example for TINI
dumpmission.class      - prebuilt Desktop class to dump a running mission
startmission.class     - prebuilt Desktop class to start a new mission
\src\dumpmission.java  - source for the mission dump example
\src\startmission.java - source for the mission start example
\tini\dumpmission.tini - prebuilt TINI class (for TINI 1.02 or later)
\tini\startmission.tini - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=---------------=

   Run each app with "-h" to see the command-line switches.

   To use the 'tini_build.bat' to rebuild this example for TINI.  Note that
   this batch file assumes it is in the specific example directory.  This batch
   file uses the environment variable TINI_HOME.  This variable must be set
   before running build batch file. For example:
       set TINI_HOME=e:\tini1.02

IV. Revision History
=-------------------=
1.00 - First release.

