Readme.txt

------------------------------------------------
| Switch Interface for the DS2408 (DS2408Demo) |
------------------------------------------------

I. Description
=------------=
DS2408 switch interface.  Turn on/off channels and
search criteria for the DS2408.

II. Files
=------=
readme.txt                    - this file
run.bat                       - batch file to run this example
build.bat                     - batch file to rebuild this example
tini_build.bat                - batch file to rebuild this example for TINI
DS2408Demo.class              - prebuilt Desktop class
\src\DS2408Demo.java          - source for this example
\tini\DS2408Demo.tini         - prebuilt TINI class (for TINI 1.02)

III. Instructions
=--------------=

Desktop:

1. Use the provided 'run.bat' to start this application.  A menu
   will be presented to select the device to do DS2408 operations.
   Another menu will then come up that provides facilities to operate
   and search the switches on the DS2408.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load DS2408Demo from the examples\DS2408Demo\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put DS2408Demo.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd)
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />

3. To run DS2408Demo type 'java DS2408Demo.tini'.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that
   this batch file assumes it is in the specific example directory.  This
   batch file uses the  environment variable TINI_HOME.  This variable must
   be set before running build batch file. For example:
       set TINI_HOME=e:\tini1.02


IV. Revision History
=-------------------=
0.00 - First release.
