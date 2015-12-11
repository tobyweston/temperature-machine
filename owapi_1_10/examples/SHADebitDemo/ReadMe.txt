Implementing SHA Transactions with the Java OneWireAPI

June 19, 2001


Purpose
-------

The purpose of this document is to familiarize users with the classes
provided by Dallas Semiconductor's Java OneWireAPI for use with SHA
iButtons.  This document should serve as both a tutorial for writing new
secure applications, as well as a walkthrough for the demo applications
shipped with the API.

This document assumes a basic understanding of the hardware involved in
implementing SHA Transactions as well as some of the basic steps involved
for using SHA iButtons for securing applications.  The data sheets for the
DS1961S/DS2432 and the DS1963S, as well as the application note for
designing secure systems with these parts, are linked to at the bottom of
this document.

More detailed documentation for the various objects used in implementing
SHA Transactions is available in the JavaDocs for the Java OneWireAPI.


Files Included
--------------

The following files should have been received as part of this demo
application:

  readme.txt          - this file

  run_debit.bat       - batch file to run debit
  run_initcopr.bat    - batch file to run initcopr
  run_initrov.bat     - batch file to run initrov

  build.bat           - batch file to rebuild this example
  tini_build.bat      - batch file to rebuild this example for TINI

  SHADebitDemo.java   - source for the debit example
  initcopr.java       - source for the co-processor initialize
  initrov.java        - source for the roving initialize
  initrov33.java      - source for the roving initialize (1961S)

  SHADebitDemo.class  - prebuilt Desktop class to debit
  initcopr.class      - prebuilt Desktop class to initialize Coprocessor
  initrov.class       - prebuild Desktop class to initialize Roving
  initrov33.class     - prebuild Desktop class to initialize Roving (1961S)

  SHADebitDemo.tini   - prebuilt TINI class (for TINI 1.02 or later)
  initcopr.tini       - prebuilt TINI class (for TINI 1.02 or later)
  initrov.tini        - prebuilt TINI class (for TINI 1.02 or later)
  initrov33.tini      - prebuilt TINI class (for TINI 1.02 or later)


Architecture Overview
---------------------

All SHA Transactions extend the SHATransaction interface from the package
com.dalsemi.onewire.application.sha.  In this package, there are two sample
transactions: SHADebit and SHADebitUnsigned.  The first implements an
account debit system, where an intial balance is stored on a user iButton
and that balance information is digitally signed using a coprocessor. When
the user is prepared to perform a transaction, the user is first
"challenged," using a challenge-response protocol, to prove it is a valid
member of the transaction system (preventing user's from copying their
money file from one button to another).  After authenticating the button,
the account data is validated by using the coprocessor to regenerate the
digital signature.  If it matches, the account file has not been tampered
with and the transaction can be performed.  After updating the account
data, a new digital signature is generated and the account file is written
back to the user device. The second transaction performs the same tasks,
but without a digital signature.  It is assumed that when using a
DS1961S/DS2432, a digital signature is superfluous since this iButton has
copy protection (i.e. the internal, unique secret must be known in order to
write data to it).

SHA Transactions have, at their heart, a SHAiButtonCopr (or, in other
words, a DS1963S that has been initialized to perform as a coprocessor in
SHA transactions).  In that sense, a SHATransaction can be thought of as an
extension to the SHAiButtonCopr.  It serves the same purpose as the
"decorator" classes in Java in that it separates the "thing to sign with"
from "what it's signing."  That way, the "thing to sign with" (i.e. the
coprocessor, SHAiButtonCopr) can be easily moved to different systems
("what it's signing") with minimal coding for the application developer. In
addition, a SHAiButtonCoprVM, or rather a simulated coprocessor, can be
used in place of a hardware DS1963S.  This class is provided as a
convenience for curbing development costs, but for optimal security an
actual hardware coprocessor is recommended.

Every method in SHATransaction takes a SHAiButtonUser as a parameter.
Although some state information might be kept in SHATransaction about a
particular user (for example, as a convenience, the user's account balance
is kept as a member of the SHADebit class after the transaction is
executed), the SHATransaction itself should not treat the user as a static
part of the system, but rather something the system acts upon.  Provided
with the API are two SHAiButtonUser objects: SHAiButtonUser33 and
SHAiButtonUser18. Although the default implementation of SHAiButtonUser
only supports the two kinds of SHA iButtons (family code 33 - DS1961S,
family code 18 - DS1963S), the concept of a user of a SHATransaction could
be extended to support any 1-Wire memory device.  The SHAiButtonUser33 and
SHAiButtonUser18 are handy extensions, however, as they both provide
authentication along with carrying the required account data.  If iButton
authentication is something that isn't important to the specific system
(for example, Door Access Control that doesn't need to log any information
on the user token), the SHAiButtonUser class could easily be extended to
support 1-Wire memory devices which would just carry signed account
information.

Note that using other memory devices as SHA iButtons only rules out the use
of a challenge-response protocol for authenticating the iButton.  There is
still a minimal level of security in that the digital signature generated
for the data could include the user token's unique, 48-bit lasered ROM ID
as well as the page number that the account info was stored on.

DS1961S Issues
--------------

The DS1961S/DS2432 is a tricky iButton to deal with.  It's smaller
scratchpad and copy-scratchpad authorization command (see the datasheet for
the DS1961S) make it somewhat cumbersome to deal with as if it were the same
(in interface only) as a DS1963S.  To ease the issue of inter-operability
between DS1963S and DS1961S user tokens in a single transaction-based
system, a few options were added to the provided demo applications.

First, since the scratchpad of the DS1961S has only 8 bytes, the message
block for the secret computation has several bytes padded with 0xFF (see
the datatsheet for the 1961S).  With the DS1963S, a full 15 bytes of
scratchpad data are included in the secret combination.  If a DS1963S wants
to be able to reproduce the unique secret for a DS1961S (for verifying an
authentication response), the system authentication secret will have to be
padded with 0xFF so that the same calculation is made by the built-in SHA
engine.  For convenience sake, a static method is provided:

class SHAiButtonCopr {
   public static byte[] reformatFor1961s(byte[]);
}

The actual constructor which initializes the SHAiButtonCopr for use in a
transaction will check the seven specific padding bytes and compare them
against 0xFF.  If they all match, the coprocessor is deemed "Compatible with
DS1961S" and has a boolean property for indicating this attribute.

A second issue of concern is the creation of the copy-scratchpad
authorization.  On the DS1961S (see the datasheet for the DS1961S), a
"Message Authentication Code," or MAC, must be generated by the application
that matches the exact MAC the iButton generates.  The calculation takes 
into account the value of the iButton's unique secret, the contents of the
data page that is being copied to, and the eight bytes which will be copied.  
If the user can produce the same MAC, the copy-scratchpad command will 
execute.  However, a quick glance at the datasheet shows that only one of 
the DS1963S SHA functions will actually let us retrieve the result of the 
caculation (all others hide the result and allow the user to attempt a 
"match") and that command is SIGN DATA PAGE.  However, the function 
SIGN DATA PAGE is only usable with one of the DS1963S's 8 secrets, secret 
zero.  In an application that uses signed transaction data, secret zero 
is already occupied with the system signing secret. If secret zero was 
replaced with the unique secret for the DS1961S, we'll destroy the 
coprocessor's ability to sign (and validate) account data.

If only a DS1961S is used as a user token, this issue isn't much of a
concern at all. The digital signature generated for data is to keep user's
from tampering with their account information.  Since the DS1961S has,
essentially, a write-protection feature, the digital signature becomes
redundant.  In this case, the coprocessor that is initialized for
authenticating the DS1963S will do a fine job of also generating the write-
authorization MAC.

If, however, a DS1963S is used as a user token as well as a DS1961S in SHA
Transactions, special consideration will need to be given to how the
DS1961S will get the required write-authorization MAC.  It could be that
the specific transaction type only does "authentication" and no "update"
(or debit) needs to be performed with the account data.  In this case, no
coprocessor needs to be provided for the DS1961S's write-authorization MAC.
Otherwise, a second coprocessor is necessary, initialized with the same
system authentication secret, for generating the write-authorization MAC.
On this coprocessor, the DS1961S unique secret will be recreated in secret
zero and the appropriate data will be signed.  The resulting signature will
be passed to the DS1961S as part of the copy-scratchpad authorization.

When using a coprocessor for generating the write-authorization MAC, pay
special care to how much and how often the transaction data is updated.
Because of the increase in I/O overhead, the time for a transaction could
increase by as much as 4 times (if there are 32 byte pages and an 8 byte
scratchpad, about 4 times as much work must be done to write a full page,
not counting the extra work done with the coprocessor).


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


initcopr Walkthrough
--------------------

The provided application, initcopr, is intended to intialize a DS1963S as a
SHAiButtonCopr, or, in other words, as a coprocessor used for
authenticating user tokens and for signing a user's account data.

The first step in the program is to load the specified adapter.  The
adapter to use, as well as the port, come from the sha.properties
configuration file.  If no adapter or port was specified, the default
adapter is used.  After loading the adapter, the application searches for a
DS1963S who will be a candidate for initialization as a coprocessor.  If a
hardcoded coprocessor address has been specified, only that address is
searched for.

After finding a valid candidate for initialization, a few configuration
questions are asked of the user.  Some of these will effect how the
coprocessor is used in the other applications, so pay special care to the
answers given for the following questions.  After each question from the
demo, there is a sample answer provided.

(1)
   Enter the name of the coprocessor file (usually 'COPR') : COPR
   Enter the file extension of the coprocessor file (0) : 0

This is the TMEX filename of the coprocessor service information file
stored on the coprocessor device.  This file provides runtime configuration
information to a transaction application.  All of the important
configuration information for the system (besides the particular secrets
used) will be saved in this file.

Note that in the sample application setup, an assumption is made that a
second coprocessor would be provided for generating the DS1961S's write-
authorization MAC (see DS1961S issues). To make it easier to find this
write-authorization coprocessor, the service file for it is named "COPR.1".

(2)
   Enter the name of the service file (4 characters) : DLSM
   Enter the file extension of the service file (102 for Money) : 102

This is the TMEX filename for the user's account info.  If a filename with
an extension of 102 is used, the TMEX file spec ensures that this account
file will be stored on a memory page which has a write-cycle counter (if
possible).


(3)
   Enter authentication page number (7) : 7

This is the page of memory used by the coprocessor for recreating a user's
unique authentication secret.  The master authentication secret is
installed for the secret corresponding to this page (if the page number is
7, then the secret number is 7).  Note that page 8 must be used for the
signing page number and this number must be high enough to allow plenty
of space for saving the coprocessor service file.

(4)
   Enter workspace page number (9) : 9

This is the page of memory used by the coprocessor for storing a user's
unique authentication secret and recreating the user's authentication
response.  Note that page 8 must be used for the signing page number and
this number must be high enough to allow plenty of space for saving the
coprocessor service file.

(5)
   Enter version number (1) : 1

This is only used by application programs at the developer's discretions.
It could be used to detect updates to a coprocessor's service file or
to match specific versions of the application to specific coprocessors.

(6)
   How would you like to enter the binding data (32 bytes)?
      1 HEX
      2 ASCII
     ? 1
    0102030405060708090A0B0C0D0E0F101112131415161718191A1B1C1D1E1F20

   How would you like to enter the binding code (7 bytes)?
      1 HEX
      2 ASCII
     ? 2
    binding

The binding data and binding code are used, along with the user token's
unique ROM ID, to produce a unique secret for a user token.  First, the
master authentication secret is installed for the user token, then this
binding data is used along with the user's account page number and the
user's unique address to "bind" the secret to the iButton.  The coprocessor
can use all of this information to recreate the user's unique secret.

(7)
   Enter a human-readable provider name:
    Maxim/DalSemi

The provider name can be used to distinguish multiple service files from
one another by a "proper name."  In that way, two service files "COPR.0"
and "COPR.1" can be installed and the provider name field could be used to
distinguish between the "Dallas-Cola" vending machine service and "Maxim-
Cola" vending machine service.

(8)
   Enter an initial signature in HEX (all 0' default):
    03010401060301040106030104010603010401060301040106

The initial signature is the value of the 20-byte digital signature that is
used when signing the account data.  The account data file has a 20-byte
space for the signature to reside after it is generated.  When generating
the signature, this information is what will reside in that spot in the
account data. Since the signature is computed across the entire account
data file, this initial signature effects the outcome of the final digital
signature.

(9)
   Enter any additional text you would like store on the coprocessor:
    Application Specific Text (optional)

Completely optional and completely application-specific text.

(10)
   Enter an encryption code (0): 0

Usually, a developer will want to add another level of security by
encrypting the user's account data as well as signing it.  This encryption
code field is used to specify the type of encryption used and should take
on application-specific values.

(11)
   How would you like to enter the signing secret (unlimited bytes)?
      1 HEX
      2 ASCII
     ? 2
    signing secret

The signing secret is installed on page 8, secret 0, of the coprocessor
device and is used to generate the digital signature for account data.
This is not stored in the coprocessor's service file.

(12)
   How would you like to enter the authentication secret (unlimited bytes)?
      1 HEX
      2 ASCII
     ? 2
    authentication secret

The authentication secret is installed on the specified page number of the
coprocessor device and is used to recreate the unique signature of a user
token for validating it's authentication response.  This is not stored in
the coprocessor's service file.


(13)
   Would you like to reformat the authentication secret for the 1961S?
   (y or n)  yes

If the answer to this question is yes, the authentication secret is
converted into a byte array with length 47 (or a multiple of 47), with
appropriate padding on the bytes that the DS1961S treats as 0xFF.  If the
answer is no, the secret is left alone.  An affirmative answer must be
given if you wish to use this coprocessor for authenticating a DS1961S.

After initialization of the coprocessor, a confirmation message is shown
along with the toString() of the object:

Initialized Coprocessor
COPR: B20000000013FB18, provider: Maxim/DalSemi, version: 1


initcopr Walkthrough - Simulated Coprocessor
--------------------------------------------

When simulating a coprocessor, the same application is run (initcopr) for
generating it's data file.  The only question that will be different is the
first.

(1)
   Would you like to emulate another coprocessor? (y) yes

If the answer to this question is yes, the application searches for the
first coprocessor it can find on the speicified coprocessor adapter/port
combindation.  It uses the specified coprocessor service filename (as
specified in sha.properties) to detect whether or not a 1963S is a valid
coprocessor.  Then, it loads all configuration info from that file and
prompts for the values of the authentication secret and the signing secret.

If the answer to the question is no, the walkthrough is exactly the same as
for the non-simulated coprocessor


initrov Walkthrough
-------------------

The sample application for intializing DS1963S user buttons for use with
SHADebit applications is "initrov." Just one simple question for this
application, since information like what the account file should be named
is stored with the specified coprocessor.

(1)
   How would you like to enter the authentication secret (unlimited bytes)?
      1 HEX
      2 ASCII
     ? 2
    authentication secret

If the coprocessor is compatible with the DS1961S, this secret is
automatically reformatted to accomadate the appropriate 0xFF padding.
After successful completion, a message is displayed to indicate that the
account data was successfully installed along with a toString() of the user
object:

Transaction data installation succeeded
USER: AE00000002657B18, service: DLSM.102, acctPageNum: 8


initrov33 Walkthrough
-------------------

The sample application for intializing DS1961S user buttons for use with
SHADebit applications is "initrov33." Just one simple question for this
application, since information like what the account file should be named
is stored with the specified coprocessor.

(1)
   How would you like to enter the authentication secret (unlimited bytes)?
      1 HEX
      2 ASCII
     ? 2
    authentication secret

If the coprocessor is not compatible with the DS1961S, they should not be
used together.  In any case, this secret is automatically reformatted to
accomadate the appropriate 0xFF padding.  After successful completion, a
message is displayed to indicate that the account data was successfully
installed along with a toString() of the user object:

Transaction data installation succeeded
USER: F3000000003BEC33, service: DLSM.102, acctPageNum: 1

Note that for service installation on a DS1961S, a write-authorization
coprocessor isn't necessary.  Since the secret is installed and "bound" to
the button by the object container, the final secret value is known in
software, so no hardware coprocessor is necessary.


SHADebitDemo Walkthrough
------------------------

When starting SHADebitDemo, the first output shown could be (depending on
various configuration options) the following initialization messages:

   Coprocessor adapter loaded, adapter: {DS9097U} port: COM1
   Looking for coprocessor: b20000000013fb18
   Loading coprocessor file: COPR.0 from device: b20000000013fb18
   COPR: B20000000013FB18, provider: Maxim/DalSemi, version: 1

This illustrates a setup where the system coprocessor is on COM1 of the
serial port.  Also, the hardcoded value of the coprocessor is shown in the
"Looking for coprocessor: xxxxxxxxxxxx" message.  After the coprocessor
is found, a confirmation message is shown.


   Write-Authorization adapter loaded, adapter: {DS9097U} port: COM1
   Looking for coprocessor: cc00000002abb718
   Loading coprocessor file: COPR.1 from device: cc00000002abb718
   COPR: CC00000002ABB718, provider: Maxim/DalSemi, version: 1

Here the write-authorization coprocessor is also on COM1, but it's hardcoded
value indicates a coprocessor other than the coprocessor.


   User adapter loaded, adapter: {DS1410E} port: LPT1
   Supporting the following family codes: 18 33

The user port is on the parallel port of the PC.  Also shown is a message
indicating that the adapter has been setup to accept both a DS1963S (family
code 0x18) and a DS1961S (family code 0x33).

   **********************************************************
      Beginning The Main Application Loop (Search & Debit)
              Press Enter to Exit Application
   **********************************************************

After this message appears, the application is an infinite loop (well,
infinite until the Enter key is pressed) waiting for new iButtons to be
place on the user adapter's bus.  Placing a DS1963S on the user port shows
the following message:

   USER: AE00000002657B18, service: DLSM.102, acctPageNum: 8
         Debit Amount: $00.50
   User's new balance: $99.50
   Total time: 310
   Executing transaction took: 60
   Verifying data took: 40
   Verifying user took: 150
   Loading user data took: 40
   Finding user took: 20

First is a toString() of the user object, showing it's address and the
page number the account file is stored on.  Next shows the amount debited
from the user's account balance, followed by the user's new balance after
the debit.  The next 6 fields specify the amount of time (in milliseconds)
each step of the transaction took to complete.  Placing a DS1961S on the
user port shows the following message:

   USER: F3000000003BEC33, service: DLSM.102, acctPageNum: 1
         Debit Amount: $00.50
   User's new balance: $99.50
   Total time: 470
   Executing transaction took: 230
   Verifying data took: 0
   Verifying user took: 140
   Loading user data took: 50
   Finding user took: 50

The layout is the same as in the previous paragraph.  The "Verifying Data"
took only zero milliseconds in this example because this setup uses unsigned
Debits for the DS1961S.



Related Documentation
---------------------

DS1961S/DS2432 datasheet - http://pdfserv.maxim-ic.com/arpdf/DS2432.pdf
DS1963S/DS2421 datasheet - http://pdfserv.maxim-ic.com/arpdf/DS1963S.pdf