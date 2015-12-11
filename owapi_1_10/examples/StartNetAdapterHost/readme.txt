Readme.txt

-------------------------------------------------
| NetAdapterHost Launcher (StartNetAdapterHost) |
-------------------------------------------------

I. Description
=------------=
Starts the host component for NetAdapter clients on the local machine.
If no options are specified, the default adapter for this machine is used
and the host is launched as a multi-threaded server using the defaults:

  Host Listen Port: 6161
  Multithreaded Host: Enabled
  Shared Secret: 'Adapter Secret Default'
  Multicast: Enabled
  Multicast Port: 6163
  Multicast Group: 228.5.6.7

syntax: java StartNetAdapterHost <options>

Options:
  -props                    Pulls all defaults from the onewire.properties
                            file rather than using the defaults set in
                            com.dalsemi.onewire.adapter.NetAdapterConstants.
  -adapterName STRING       Selects the Adapter to use for the host.
  -adapterPort STRING       Selects the Adapter port to use for the host.
  -listenPort NUM           Sets the host's listening port for incoming
                            socket connections.
  -multithread [true|false] Sets whether or not the hosts launches a new
                            thread for every incoming client.
  -secret STRING            Sets the shared secret for authenticating incoming
                            client connections.
  -multicast [true|false]   Enables/Disables the multicast listener. If
                            disabled, clients will not be able to
                            automatically discover this host.
  -multicastPort NUM        Set the port number for receiving packets.
  -multicastGroup STRING    Set the group for multicast sockets.  Must be in
                            the range of '224.0.0.0' to '239.255.255.255'.


II. Files
=------=
readme.txt                      - this file
run.bat                         - batch file to run this example
build.bat                       - batch file to rebuild this example
tini_build.bat                  - batch file to rebuild this example for TINI
StartNetAdapterHost.class       - prebuilt Desktop class
\src\StartNetAdapterHost.java   - source for this example
\tini\StartNetAdapterHost.tini  - prebuilt TINI class (for TINI 1.02 or later)

III. Instructions
=--------------=

Desktop:

1. Use the provided 'run.bat' to start this application.
   It will automatically search for the Default 1-Wire port and launch
   the NetAdapterHost component with this port.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.

TINI (1.02 or later):

1. Load StartNetAdapterHost from the examples\StartNetAdapterHost\tini directory
   using FTP. First open FTP and execute the following commands (for the sake
   of this discussion assume you chose 180.0.42.43 for your IP address):

       open 180.0.42.43
       root
       tini
       bin
       put StartNetAdapterHost.tini
       close
       quit

  In Windows 9x you can place these commands in a script file (e.g. doftp.cmd)
  and execute with 'ftp -s:doftp.cmd'.

2. Open a Telnet session to your TINI board.

    Welcome to slush.  (Version 1.02)

    TINI login: root
    TINI password:
    TINI />

3. To get command line systax execute ReadTemp without any parameter
   'java StartNetAdapterHost.tini'.  Parameters can be added as described above
   under Desktop.

4. Use the 'tini_build.bat' to rebuild this example for TINI.  Note that this
   batch file assumes it is in the specific example directory.  This batch
   file uses the  environment variable TINI_HOME.  This variable must be set
   before running build batch file. For example:
       set TINI_HOME=e:\tini1.02

IV. Revision History
=-------------------=
1.00B - First release.
