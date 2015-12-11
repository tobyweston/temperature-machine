Readme.txt

---------------------------------------
| 1-Wire Network Watcher (OWNetWatch) |
---------------------------------------

I. Description
=------------=
Monitor the arrival and departure of all 1-Wire devices on a 
complex network utilizing the DS2409 Coupler to create branches.  
Also starts a thread to read the temperature of any DS1920/DS1820 
devices found on the network.

II. Files
=------=
readme.txt            - this file
run.bat               - batch file to run this example
build.bat             - batch file to rebuild this example
tini_build.bat        - batch file to rebuild this example for TINI
OWNetWatch.class      - main prebuilt Desktop class 
TempWatch.class       - prebuilt Desktop class 
\src\OWNetWatch.java  - main source for this example
\src\TempWatch.java   - temperature watcher source for this example 
\tini\OWNetWatch.tini - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=--------------=

Desktop: 

1. Use the provided 'run.bat' to start this application.  This application
   accepts a command line argument of the number of milliseconds that
   the 1-Wire network will be monitored.  For example 'run 6000' will run
   for 6 seconds.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load OWNetWatch from the examples\OWNetWatch\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put OWNetWatch.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. To get command line systax execute OWNetWatch without any parameter
   'java OWNetWatch.tini'.  Parameters can be added as described above
   under Desktop.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that 
   this batch file assumes it is in the specific example directory.  This 
   batch file uses the  environment variable TINI_HOME.  This variable must 
   be set before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02

IV. Revision History
=-------------------=
0.00 - First release.
