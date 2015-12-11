Readme.txt

--------------------------------------------------------------
| Find All 1-Wire Device's Application (FindiButtonsConsole) |
--------------------------------------------------------------

I. Description
=------------=
Simple application to enumerate through all 1-Wire adapters
and ports and list the devices present.

II. Files
=------=
readme.txt                      - this file
run.bat                         - batch file to run this example
build.bat                       - batch file to rebuild this example
tini_build.bat                   - batch file to rebuild this example for TINI
FindiButtonsConsole.class       - prebuilt Desktop class
\src\FindiButtonsConsole.java   - source for this example
\tini\FindiButtonsConsole.tini  - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=--------------=

Desktop: 

1. Use the provided 'run.bat' to start this application.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load FindiButtonsConsole from the examples\FindiButtonsConsole\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put FindiButtonsConsole.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. To run FindiButtonsConsole type 'java FindiButtonsConsole.tini'. 

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that this 
   batch file assumes it is in the specific example directory.  This batch 
   file uses the  environment variable TINI_HOME.  This variable must be set 
   before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02

   'tini_build.bat' compiles with no 1-Wire container dependencies. If you
   want this application to provided device specific descriptions of 
   the 1-Wire devices found, add the dependencies.


IV. Revision History
=-------------------=
2.00 - Nefarious

