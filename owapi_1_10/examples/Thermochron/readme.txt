Readme.txt

-----------------------------------------
| DS1921 Thermochron Demo (Thermochron) |
-----------------------------------------

I. Description
=------------=
DS1921 temperature gathering demo consisting of two applications.  
One application 'missions' a DS1921 to collect data and the other 
'dumps' the mission data.

II. Files
=-------=
readme.txt             - this file
run_dumpMission.bat    - batch file to run dumpMission
run_initMission.bat    - batch file to run initMission
build.bat              - batch file to rebuild this example
tini_build.bat         - batch file to rebuild this example for TINI
dumpMission.class      - prebuilt Desktop class to dumpMission 
initMission.class      - prebuilt Desktop class to initialize Mission
\src\dumpMission.java  - source for the dumpMission example
\src\initMission.java  - source for the mission initialize
\tini\dumpMission.tini - prebuilt TINI class (for TINI 1.02 or later)
\tini\initMission.tini - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=---------------=

Desktop: 

DS1921 Thermocron Java Demo: Mission Reading Program

This demo has several command line options that may be hard
to discover through normal use of the program.  These options 
are:

   a  Print the alarm violation history for the mission.
   h  Print the histogram for the mission.
   l  Print the log of temperatures for the mission.
   k  Kill the mission before the program gathers its data
   s  Stop the mission after the program gathers its data

For example, you might use the command line:

    java dumpMission alk

The output would report any alarm violations that occurred,
the entire log of temperatures (up to the last 2048 logs),
and it would end the current mission before reading this 
data. 

TINI (1.02 or later):

1. Load SHA from the examples\SHA\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put dumpMission.tini
       put initMission.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. See the Desktop section for usage.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that this 
   batch file assumes it is in the specific example directory.  This batch 
   file uses the  environment variable TINI_HOME.  This variable must be set 
   before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02

IV. Revision History
=-------------------=
0.00 - First release.
       
