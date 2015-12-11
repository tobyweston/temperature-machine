Readme.txt

------------------------------------------------------------------
| 1-Wire XML Tag Viewer (TagViewer) and Tag Creator (TagCreator) |
------------------------------------------------------------------

I. Description
=------------=
Java Swing applications that exercise the 1-Wire XML tagging
libraries.  The Tag Viewer application spawns a thread for
every tagged device it finds and can read sensors and log the
results.  It also has settable polling features.  For actuators,
it can supply the state selections for them, and exercise the
chosen selection. It takes XML tag files as input, and if XML
tag files reside on the 1-Wire devices, it can read and parse
those after the program has started.

The TagCreator is a small swing application wizard that
prompts the user for input to create a 1-Wire XML tag file. It
displays selections of elements for the user to choose on
each step and prompts for the pertinent attributes.  When
finished it asks to user where to write the file. The resulting
XML document can be written to either a file on disk or to
a 1-Wire device.

For more documentation on XML 1-Wire tagging, please see App Note
158:   http://pdfserv.maxim-ic.com/arpdf/AppNotes/app158.pdf

II. Files
=------=
readme.txt                      - this file
build.bat                       - batch file to rebuild this example
run.bat                         - batch file to run the TagViewer application
runTagCreator.bat               - batch file to run the TagCreator application
branch.xml                      - example branch tag file
contact.xml                     - example contact tag file
humidity.xml                    - example humidity tag file
thermal.xml                     - example thermal tag file
cluster.xml                     - example cluster tag file
event.xml                       - example event tag file
switch.xml                      - example switch tag file
d2a.xml                         - example d2a tag file
TagViewer.class                 - main prebuilt Desktop class for TagViewer
TagMainFrame.class              - prebuilt Desktop class for TagViewer
DeviceFrame.class               - prebuilt Desktop class for TagViewer
DeviceFrame$1.class             - prebuilt Desktop class for TagViewer
DeviceFrameActuator.class       - prebuilt Desktop class for TagViewer
DeviceFrameSensor.class         - prebuilt Desktop class for TagViewer
DeviceMonitorActuator.class     - prebuilt Desktop class for TagViewer
DeviceMonitorSensor.class       - prebuilt Desktop class for TagViewer
ParseContainer.class            - prebuilt Desktop class for TagViewer
TagCreator.class                - main prebuilt Desktop class for TagCreator
\src\TagViewer.java             - main source for TagViewer
\src\TagMainFrame.java          - TagMainFrame source for TagViewer
\src\DeviceFrame.java           - DeviceFrame source for TagViewer
\src\DeviceFrameActuator.java   - DeviceFrameActuator source for TagViewer
\src\DeviceFrameSensor.java     - DeviceFrameSensor source for TagViewer
\src\DeviceMonitorActuator.java - DeviceMonitorActuator source for TagViewer
\src\DeviceMonitorSensor.java   - DeviceMonitorSensor source for TagViewer
\src\ParseContainer.java        - ParseContainer source for TagViewer
\src\TagCreator.java            - main source for TagCreator


III. Instructions
=---------------=

Desktop:

1. Make sure that the MinML SAX Parser is installed (MinML.jar).  MinML can
   be downloaded here:  http://www.minml.com.  Make sure MinML.jar and
   OneWireAPI.jar are in the appropriate directory (located in the
   <JRE_HOME>/lib/ext) or are entries in the classpath environment variable.
   Another SAX XML Parser can be used (although MinML is the default if
   none is specified) by adding a property to the onewire.properties field.
   The property SAXParser.ClassName should be the fully qualified classname
   of an implementation of org.xml.sax.Parser.

   Then, run TagViewer by typing "java TagViewer" at the command prompt.  Or
   run it with 1-Wire XML tag files as input parameters, such as "java
   TagViewer contact.xml" or "java TagViewer event.xml contact.xml".

   Alternately, one can use the provided 'run.bat' to start this application. It
   accepts a command line argument of XML tag file names.  The batch file
   accepts up to 8 names.

   For TagCreator, run it by typing "java TagCreator" at the command prompt.
   Again, make sure that the OneWireAPI.jar file is in the appropriate
   directory.  Alternately, run the provided "runTagCreator.bat" file.

2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar and MinML.jar files in your
   default classpath or in <JDK_HOME>\jre\lib\ext.


IV. Revision History
=-------------------=
0.00 - First release.
