Readme.txt

----------------------------------------------------------------
| 1-Wire 1K-Bit protected EEPROM with SHA-1 Engine (DemoSHAEE) |
----------------------------------------------------------------


I. Description
=------------=

Utility to excise the SHA operations of a DS1961S/DS2432 


II. Files
=------=

readme.txt           - this file
run.bat              - batch file to run this example
build.bat            - batch file to rebuild this example
tini_build.bat       - batch file to rebuild this example for TINI
\src\DemoSHAEE.java  - source for this example
\tini\DemoSHAEE.tini - prebuilt TINI class (for TINI 1.02 or later)


III. Instructions
=--------------=

Desktop: 

1. Use the provided 'run.bat' to start this application.
   To get command line syntax execute DemoSHAEE without any parameter
   'run'.  You will see the following output, just an example:

     Device Selection
     (0) A5004770000137B3 - DS1961S/DS2432
     [1]--Quit


2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load DemoSHAEE from the examples\DemoSHAEE\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put DemoSHAEE.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd) 
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />   

3. To get command line systax execute DemoSHAEE without any parameter
   'java DemoSHAEE.tini'.  Parameters can be added as described above
   under Desktop.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that 
   this batch file assumes it is in the specific example directory.  This 
   batch file uses the  environment variable TINI_HOME.  This variable must 
   be set before running build batch file. For example:  
       set TINI_HOME=e:\tini1.02


IV. Usage description
=-------------------=

1.  Make sure the secret is set by either changing the bus master secret or
    actually loading first secret.  This should be done first and then you can
    write to the part without the default secret being used.  The loading
    first secret command writes the secret you provide to the secret location
    on the part, given that the secret is not already locked.  The bus master
    secret is just what the program uses to compute the MAC to match the
    button's calculated MAC.  The next secret command calculates a new secret 
    based on the current secret and a 'next' secret.  This allows the 
    implementation of a 'partial secret' arrangement.

2.  The status page can not be read authenticated.  The status page is where 
    the hidden secret and the status of the pages are located along with the 
    1-Wire network address.

3.  Page 0 can be write protected by itself along with all the other memory
    pages, but it is not set on new buttons.

4.  Page 1 can be changed to EPROM mode and can be write protected along
    with all of the pages.

5.  Page 2 and 3 can be write protected along with the other pages.

6.  In case you are not sure what SHA-1 is, here is a definition:
    Secure Hash Algorithm-1 (SHA-1)

    A one-way cryptographic function which takes a message of
    less than 18 quintillion (18,446,744,073,709,551,616) bits
    in length and produces a 160-bit message digest. A message
    digest is a value generated for a message or document that
    is unique to that message, and is sometimes referred to as a
    "fingerprint" of that message or data. Once a message digest
    is computed, any subsequent change to the original data
    will, with a very high probability, cause a change in the
    message digest, and the signature will fail to verify. This
    process is used to compress large data strings to a 20-byte
    length which is used in a cryptographic process. The reduced
    data length relieves computational requirements for data
    encryption. SHA-1 provides greater security than the
    original Secure Hash Algorithm (SHA), which had security
    flaws.

    The above definition can be found here:
    http://home.earthlink.net/~neilbawd/sha1.html

    Also the federal government for the secure hash algorithm is located
    here on the web:  http://www.itl.nist.gov/fipspubs/fip180-1.htm

7.  The MAC that is calculated, is a 20 byte array that uses a 64 byte 
    array input that is built from the page data, secret, scratchpad 
    and a few various hard coded values.  This 64 byte array is passed
    into the SHA-1 Engine.  See the device datasheet for the specific
    format of this array.
 

V. A Simple "Walk-Through" Example:
=---------------------------------=

This example is a walk-through in setting up a DS2432/DS1961S to read 
and write with SHA-1 authentication.  This walk-through assumes you have 
a part that is not locked.

1.  Start by running the program on a command line (dos prompt):

    D:\owapi\examples\DemoSHAEE\run

2.  The following is returned:

    Device Selection
    (0) 2000474000021BB3 - DS1961S/DS2432
    [1]--Quit
    Please enter value:

3.  In this example, we have just one part on the bus, so choose it by 
    typing in 0 and <enter>.

4.  The program responds by displaying the main menu:

    Main Menu
    (0)  Read/Write Memory Bank
    (1)  Load First Secret
    (2)  Compute Next Secret
    (3)  Change Bus Master Secret
    (4)  Lock Secret
    (5)  Input new challenge for Read Authenticate
    (6)  Write Protect page 0-3
    (7)  Set Page 1 to EEPROM mode
    (8)  Write Protect Page 0
    (9)  Print Current Bus Master Secret
    (10) Quit
    Please enter value:

5.  Let's choose "Load First Secret".  Loading the first secret will set 
    the secret used by the SHA-1 engine in the part.  This information 
    cannot be retrieved from the part once it has been set, so it is a 
    good idea to remember it!  When developing a program with the 
    DS1961S/DS2432 in mind, it is highly recommended to lock this secret 
    in the part to help thwart potential hackers.  Choosing "Load First 
    Secret" from the main menu returns:

    Enter the 8 bytes of data to be written.
    AA AA AA AA AA AA AA AA  <- Example

    Data Entry Mode
    (0) Text (single line)
    (1) Hex (XX XX XX XX ...)
    Please enter value:

6.  For this example, let's choose "Text" by typing in 0 followed by <enter>.
    The program responds by returning the cursor to the beginning of a line.  
    Now, type in an example string "secret" followed by <enter>.  The program 
    sends an 8-byte ASCII representation of "secret" to the part right-padded 
    with 0s as shown below:  

    Please enter value: 0
    secret
    Data to write, len (8) :7365637265740000
    First Secret was Loaded.

    Congratulations!  The "First Secret" has been successfully loaded.  Please 
    note that in loading the "First Secret" to the part, the program also 
    sets the "Bus Master Secret" automatically.  So, you can actually skip 
    steps 7 and 8 (but it won't hurt to do steps 7 and 8 to show an example 
    of how to set the secret).

7.  This step is to set the "Bus Master Secret".  The program automatically 
    displays the main menu after the previous operation.  The "Bus Master 
    Secret" is the secret that the 1-Wire bus master keeps.  In this case, 
    the bus master is the DemoSHAEE program.  It should be the same as the 
    secret in the part (i.e., the "First Secret").  If the 2 secrets do not 
    match, the SHA-1 calculated in the part and in the program will not match, 
    giving errors.  So, choose option 3 from the main menu, and the program 
    returns:

    Enter the 8 bytes of data to be written.
    AA AA AA AA AA AA AA AA  <- Example

    Data Entry Mode
    (0) Text (single line)
    (1) Hex (XX XX XX XX ...)
    Please enter value:

8.  Again, let's choose "Text", and enter in the string "secret" just as in 
    step 6 above.  The response should be:

    Please enter value: 0
    secret
    Data to write, len (8) :7365637265740000
    Bus Master Secret Changed.

9.  The "Bus Master Secret" has now been read into the program.  The program 
    automatically displays the main menu after the previous operation.  The 
    next step is to input a new challenge for the "Read Authenticate" process.  
    Ideally, this challenge should change each time it is issued by the bus master.  
    For this example, let's enter the string "hackthis" as the new challenge.  
    Choose "Input new challenge for Read Authenticate" from the main menu.  
    The program returns:

    Enter 8 bytes for the challenge
    AA AA AA AA AA AA AA AA  <- Example

    Data Entry Mode
    (0) Text (single line)
    (1) Hex (XX XX XX XX ...)
    Please enter value:

10. Again, let's choose "Text", and enter in the string "hackthis" just as in 
    step 6 above.  The response should be: 

    Please enter value: 0
    hackthis
    Data to write, len (8) :6861636B74686973

    The ASCII representation of "hackthis" is stored in the DemoSHAEE program to
    be placed in the SHA-1 calculations and the main menu is displayed.

    It is now possible to read and write to the part with SHA-1 authentication...


VI. Revision History
=-------------------=

0.00 - First release.

