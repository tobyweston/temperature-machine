Readme.txt

---------------------------------------
| 1-Wire Memory Dump Utility (OWDump) |
---------------------------------------

I. Description
=------------=
Dump the memory contents of a all of the of 1-Wire devices on the
default 1-Wire network in three selectable formats (raw, pages, packets).


II. Files
=------=
readme.txt        - this file
run.bat           - batch file to run this example
build.bat         - batch file to rebuild this example
tini_build.bat    - batch file to rebuild this example for TINI
OWDump.class      - prebuilt Desktop class
\src\OWDump.java  - source for this example
\tini\OWDump.tini - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=--------------=

Desktop: 

1. Use the provided 'run.bat' to start this application.
   To get command line syntax execute OWDump without any parameter
   'run'.  You will see the following output:

     OneWire Memory Dump console application: Version 0.00
     Arch: TINI,  OS Name: slush,  OS Version: TINI OS 1.02

     syntax: OWDump ('r' 'p' 'k') <TIME_TEST>
        Dump an iButton/1-Wire Device contents
        'r' 'p' 'k'  - required flag: (Raw,Page,pacKet) type dump
        <TIME_TEST>  - optional flag if present will time each read
                      of memory banks and not display the contents

2. Raw 'r' mode will display the contents of all memory banks
   present.  Page 'p' and packet 'k' will display attempt to
   read only the memory banks that are read/write and general purpose.

   Here is some sample output.  The Adapter information is displayed
   with "==", the device information with "*" and the memory bank 
   information with "|".  For example, 'run r':

     OneWire Memory Dump console application: Version 0.00
     Arch: TINI,  OS Name: slush,  OS Version: TINI OS 1.01

     =========================================================================
     == Adapter Name: TINIExternalAdapter
     == Adapter Port description: <na>
     == Adapter Version: <na>
     == Adapter support overdrive: true
     == Adapter support hyperdrive: false
     == Adapter support EPROM programming: false
     == Adapter support power: true
     == Adapter support smart power: false
     == Adapter Class Version: 1.01

     *************************************************************************
     * 1-Wire Device Name: DS1996
     * 1-Wire Device Other Names:
     * 1-Wire Device Address: 150000000140D60C
     * 1-Wire Device Max speed: Overdrive
     * 1-Wire Device Description: 65536 bit read/write nonvolatile memory partitioned
      into two-hundred fifty-six pages of 256 bits each.
     ...

3. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load OWDump from the examples\OWDump\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put OWDump.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. To get command line systax execute OWDump without any parameter
   'java OWDump.tini'.  Parameters can be added as described above
   under Desktop.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that 
   this batch file assumes it is in the specific example directory.  This 
   batch file uses the  environment variable TINI_HOME.  This variable must 
   be set before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02

   'tini_build.bat' compiles in all known 1-Wire devices with memory.  
   The end result .tini file can be reduced in size if only the 1-Wire 
   Containers needed are included.

IV. Revision History
=-------------------=
0.00 - First release.
0.01 - Changed to non-deprecated PagedMemoryBank.hasExtraInfo()
