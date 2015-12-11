Implementing SHA Remote Authentications with the Java OneWireAPI

June 19, 2001


Purpose
-------

The purpose of this document is to familiarize users with the classes
provided by Dallas Semiconductor's Java OneWireAPI for use with SHA
Remote Authentication.  

This document assumes a basic understanding of the hardware involved in
implementing SHA remote authentications as well as some of the basic steps
involved for using SHA iButtons for securing applications.  The data 
sheets for the DS1961S/DS2432 and the DS1963S, as well as the application 
note for designing secure systems with these parts, are linked to at the 
bottom of this document.

More detailed documentation for the various objects used in implementing
SHA Transactions is available in the JavaDocs for the Java OneWireAPI.


Files Included
--------------

The following files should have been received as part of this demo
application:

  readme.txt          - this file

  run_AuthHost.bat    - batch file to run runHost
  run_AuthUser.bat    - batch file to run AuthenticateUser

  build.bat           - batch file to rebuild this example
  tini_build.bat      - batch file to rebuild this example for TINI

  AuthenticateUser.java        - source for the user in authenticating
  AuthenticationConstants.java - constants and types used for remote
                                 authentication
  AuthenticationHost.java      - source for authenticating different
                                 users
  runHost.java                 - source for starting up the host

  AuthenticateUser.class        - prebuilt Desktop class for user
                                  authentication
  AuthenticationConstants.class - prebuilt Desktop class for constants
                                  used in authentication
  AuthenticationHost.class      - prebuild Desktop class for authenticating
                                  different users
  runHost.class                 - prebuilt Desktop class for running the 
                                  host

  AuthenticateUser.tini        - prebuilt TINI class (for TINI 1.02 or later)
  AuthenticationConstants.tini - prebuilt TINI class (for TINI 1.02 or later)
  AuthenticationHost.tini      - prebuilt TINI class (for TINI 1.02 or later)
  runHost.tini                 - prebuilt TINI class (for TINI 1.02 or later)


Setup
-----
First the Authentication Host uses a initialized virtual machine coprocessor,
which is initialized by using initcoprvm.  The User has to be initialized with
initrov or initrov33.  For instructions on initializing check out the 
SHADebitDemo ReadMe.txt.  The Host connection should be setup first before 
running the User connection.

Architecture Overview
---------------------

The AuthenticationHost class takes in a SHAiButtonCopr that is the coprocessor
that is used for authenticating the User, a port on which the User will be 
connecting and a secret to check the connection.  The run method in the 
AuthenticationHost class is used to loop through all the connections and use
the secret to verify the connection.  If the connection is valid then 
authenticate is called to authenticate the remote connection.  The authenticate
method calls verifyUserNet and verifyTransactionDataNet, which are similar
to verifyUser and verifyTransactionData in the SHATransaction class but use
the network connection to get the data that is need for verification.

The AuthenticateUser main funtion just connects to the Host, which is a user
input, and sends the Serial Number of the device that is to be authenticated.  
It then just retrieves information for the Host.  The user is kept up to date
on the progress of the authentication by printed messages.

The AuthenticationConstants constants are the commands sent to the user and 
the data type for the connection made between the Host and the User.

The runHost just sets up the coprocessor information and initializes 
AuthenticationHost and calls the run method.


Using sha.properties
--------------------

All of the key-value pairs in sha.properties should be well commented as to
what their intended use is and what valid options exist for the value. This
properties file is the source for all configuration info for the provided
demo programs.  If, in the following walkthroughs of the different demo
applications, the term "the specified value for" or another similar phrase
is used, the term refers to a key-value pair in the sha.properties file.

All of the provided sample applications allow the option of specifying the
full path to the sha.properties file on the command-line.  This can be used
to specify different setups for debugging.  For example, the following
command-line would load all properties from a file name
"debug.sha.properties" located in the current folder:

   java initcopr -p .\debug.sha.properties

Optionally, this feature can be used to specify different services:

   java initcopr -p .\DoorAccess.sha.properties
   java initcopr -p .\VendingMachine.sha.properties

Before starting up any of the sample applications, browse the
sha.properties file to make sure that all of the configuration options make
sense for the environment they will be run in.


runHost Walkthrough
--------------------

The provided application, runHost, is intended to intialize the coprocessor
for the remote authentication and get the port to listen on setup.  The 
virtual machine coprocessor should already be setup before starting
this program.

After the initial information for the coprocessor:

example:
COPRVM: B20000000013FB18, provider: mxim, version: 1

The Host just wants for a connection and prints out the Serial Number,
the data in the file from the User and whether or not the authentication
was successful.

example:
Remote Authentication Successful for part:
18.4A.A9.02.00.00.00.77.
With the following verification data:
74.65.73.74.69.6E.67.  .  .  .  .  .  .  .  .  .  testing


AuthenticateUser Walkthrough
----------------------------

The provided application, AuthenticateUser, is intended to connect to the
Host and verify the User device.

Here is a sample of the input for the server and the verification process:
Enter the Host server you want to connect to.
jevans.dalsemi.com

Connected to Host and now receiving bind code,
bind data and file name.

Receiving challenge for authentication

Sending the user verification data.

Sending the verify transaction data.

End of authentication reached.


Related Documentation
---------------------

DS1961S/DS2432 datasheet - http://pdfserv.maxim-ic.com/arpdf/DS2432.pdf
DS1963S/DS2421 datasheet - http://pdfserv.maxim-ic.com/arpdf/DS1963S.pdf