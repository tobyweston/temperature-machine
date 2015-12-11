Readme.txt

-------------------------------------------------
| 1-Wire Viewer GUI Application (OneWireViewer) |
-------------------------------------------------

I. Description
=------------=

A GUI application which exercises the features of every 1-Wire device
supported by the 1-Wire API for Java.  A graphical 'viewer' is provided for
each of the following 1-Wire device types: A-to-D, Real-time Clock,
Humidity, Memory, Potentiometer, Switch, and Temperature.  In addition, a
viewer is provided for exercising the extra features of the Thermochron, the
DS1963S SHA, and the DS1961S/DS2432 SHA iButtons.  All devices with general-
purpose memory can utilize a 1-Wire filesystem with a provided viewer as
well.

Note that this Demo application utilizes advanced features of the Java Swing
API, therefore a minimum JRE version of 1.3.1 is required to run this
application.


II. Files
=------=
readme.txt        - this file
run.bat           - batch file to run this example
build.bat         - batch file to rebuild this example
OneWireAPI.jar    - prebuilt Desktop jar
src\*.java        - source for this example

III. Instructions
=--------------=

Desktop:

1. Use the provided 'run.bat' to start this application. The OneWireViewer
   will automatically attempt to load the default adapter.  If it fails, an
   Adapter Chooser dialog will appear.  Select the default adapter you wish
   to use and press "Load Adapter" followed by the OK button.


2. Use the 'build.bat' to rebuild this example.  Note that this batch
   file assumes it is in the specific example directory.  It is recommended
   for ease of use to put the OneWireAPI.jar file in your default classpath
   or in <JDK_HOME>\jre\lib\ext.


IV. Revision History
=-------------------=
1.00 - First release.
