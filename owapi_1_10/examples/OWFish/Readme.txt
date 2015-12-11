Readme.txt

-------------------------------------
| 1-Wire File System Shell (OWFish) |
-------------------------------------

I. Description
=------------=
1-Wire File System Shell.  Read, write, format and display the 
file system on a 1-Wire memory device.

II. Files
=------=
readme.txt                - this file
run.bat                   - batch file to run this example
build.bat                 - batch file to rebuild this example
tini_build.bat            - batch file to rebuild this example for TINI
OWFish.class              - prebuilt Desktop class
\src\OWFish.java          - source for this example
\tini\OWFish.tini         - prebuilt TINI class (for TINI 1.02)
\tini\OWFish_0C_only.tini - prebuilt TINI class (for TINI 1.02)

III. Instructions
=--------------=

Desktop: 

1. Use the provided 'run.bat' to start this application.  A menu
   will be presented to select the device to do 1-Wire file 
   system operations on.  Another menu will then come up that
   provides facilities to read/write the 1-Wire file system.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load OWFish from the examples\OWFish\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put OWFish.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. To run OWFish type 'java OWFish.tini'. 

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that 
   this batch file assumes it is in the specific example directory.  This 
   batch file uses the  environment variable TINI_HOME.  This variable must 
   be set before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02

   'tini_build.bat' compiles in all known 1-Wire devices with memory.
   It also builds a reduced size file that contains only the DS1996 (0C)
   1-Wire container called 'OWFish_0C_only.tini'.

IV. Revision History
=-------------------=
0.00 - First release.
0.01 - Added selection of multiple devices at a time so that a 1-Wire file system
       could be created across all of the devices.  
