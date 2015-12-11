
/*---------------------------------------------------------------------------
 * Copyright (C) 2001 Dallas Semiconductor Corporation, All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY,  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL DALLAS SEMICONDUCTOR BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * Except as contained in this notice, the name of Dallas Semiconductor
 * shall not be used except as stated in the Dallas Semiconductor
 * Branding Policy.
 *---------------------------------------------------------------------------
 */

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.application.file.OWFileInputStream;
import com.dalsemi.onewire.application.file.OWFileNotFoundException;
import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.utils.OWPath;
import java.io.*;
import java.util.*;
import org.xml.sax.SAXException;

/**
 * Main class for a SWING based application that dynamically reads
 * XML 1-Wire Tags and displays and appropriate window for each
 * 'sensor' or 'actuator' found.  If a 'branch' is found then it
 * is added to the 1-Wire Search Path for looking for more XML
 * files.  Note that XML files can also be supplied from the
 * system by providing 1 or more on the command line.  The default
 * 1-Wire adapter is used.
 *
 * TODO
 *    1. Make change in log file push change to sub-viewers
 *    2. Show list of sub-viewers active
 *    3. Allow closing of some sub-viewers without ending application
 *    4. Remember window locations and options
 *    5. Select 1-Wire adapter instead of default
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class TagViewer
{
   //--------
   //-------- Variables
   //--------

   /** XML parser */
   public static TAGParser parser;

   /** 1-Wire adapter that will be used for communication */
   public static DSPortAdapter adapter;

   /**
    * Hashtable to keep track of what 1-Wire devices have already
    * been examined for XML files
    */
   public static Hashtable parseLog;

   /** Vector of devices that have been 'tagged' */
   public static Vector taggedDevices;

   /** 1-Wire search paths */
   public static Vector paths;

   /** Vector of windows created for each 'tagged' device */
   public static Vector deviceFrames;

   /** Logfile name string */
   public static String logFile;

   /** Main window for this application */
   public static TagMainFrame main;

   //--------
   //-------- Methods
   //--------

   /**
    * Method main that creates the main window and then polls for
    * for XML files.
    *
    * @param args command line arguments
    */
   public static void main(String[] args)
   {
      int path_count=0;

      try
      {
         // attempt to get the default adapter
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // create the state instances
         parseLog       = new Hashtable();
         taggedDevices  = new Vector();
         paths          = new Vector();
         deviceFrames   = new Vector();

         // create dummy 'trunk' search path
         paths.addElement(new OWPath(adapter));

         // create the main frame
         main = new TagMainFrame();
         main.setAdapterLabel(adapter.getAdapterName() + "_" + adapter.getPortName());

         // get the initial log file
         logFile = main.getLogFile();

         // check for XML files on the command line
         for (int i = 0; i < args.length; i++)
         {
            main.setStatus("File being parsed:  " + args[i]);
            FileInputStream file_stream = new FileInputStream(args[i]);

            // create the tagParser
            parser = new TAGParser(adapter);

            // attempt to parse it
            parseStream(parser, file_stream, new OWPath(adapter), true);
         }

         // add the paths to the main window
         main.clearPathList();
         for (int p = 0; p < paths.size(); p++)
            main.addToPathList(((OWPath)paths.get(p)).toString());

         // turn off all branches
         allBranchesOff();

         // run loop
         for (;;)
         {
            // check if scanning 1-Wire for XML files enabled
            if (main.isScanChecked())
            {
               // check if there is a path to search
               if (path_count < paths.size())
               {
                  // only increment if there is nothing else to search for
                  if (pathXMLSearchComplete(path_count))
                     path_count++;
               }
               else
                  path_count = 0;
            }

            // sleep for 1 second
            main.setStatus("sleeping");
            try
            {
              Thread.sleep(1000);
            }
            catch (InterruptedException e){}
         }
      }
      catch (RuntimeException ex)
      {
         throw ex;
      }
      catch (Exception ex)
      {
         ex.printStackTrace();
      }
   }

   /**
    * Search a given path for an XML file.
    *
    * @param currentPathIndex index into the 'paths' Vector that
    *        indicates what 1-Wire path to search for XML files
    * @return true if the current path provided has been
    *         completely checked for XML files.  false if
    *         if this current path should be searched further
    */
   public static boolean pathXMLSearchComplete(int currentPathIndex)
   {
      OneWireContainer owd = null, check_owd = null;
      ParseContainer pc = null, check_pc = null;
      OWFileInputStream file_stream = null;
      boolean rslt = true;
      boolean xml_parsed = false;

      try
      {
         main.setStatus("Waiting for 1-Wire available");

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         main.setStatus("Exclusive 1-Wire aquired");

         // open the current path to the device
         OWPath owpath = (OWPath)paths.get(currentPathIndex);
         owpath.open();

         main.setStatus("Path opened: " + owpath.toString());

         // setup search
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // find all devices, update parseLog and get a device filesystem to check
         for(Enumeration owd_enum = adapter.getAllDeviceContainers();
                         owd_enum.hasMoreElements(); )
         {
            owd = (OneWireContainer)owd_enum.nextElement();
            Long key = new Long(owd.getAddressAsLong());

            main.setStatus("Device Found: " + owd.getAddressAsString());

            // check to see if this is in the parseLog, add if not there
            pc = (ParseContainer)parseLog.get(key);
            if (pc == null)
            {
               main.setStatus("Device is new to parse: " + owd.getAddressAsString());
               pc = new ParseContainer(owd);
               parseLog.put(key,pc);
            }
            else
               main.setStatus("Device " + owd.getAddressAsString() + " with count " + pc.attemptCount);

            // if attemptCount is low then check it later for XML files
            if (pc.attemptCount < ParseContainer.MAX_ATTEMPT)
            {
               check_owd = owd;
               check_pc = pc;
            }
         }

         // check if there is anything to open
         if (check_owd != null)
         {
            // result is false because found something to try and open
            rslt = false;

            main.setStatus("Attempt to open file TAGX.000");

            // attempt to open a 'TAGX.000' file, (if fail update parse_log)
            try
            {
               file_stream = new OWFileInputStream(check_owd,"TAGX.0");
               main.setStatus("Success file TAGX.000 opened");
            }
            catch (OWFileNotFoundException fex)
            {
               file_stream = null;
               check_pc.attemptCount++;
               main.setStatus("Could not open TAGX.000 file");
            }
         }

         // try to parse the file, (if fail update parse_log)
         if (file_stream != null)
         {
            // create the tagParser
            // (this should not be necessary but the parser currently holds state)
            parser = new TAGParser(adapter);

            // attempt to parse it, on success max out the attempt
            if (parseStream(parser, file_stream, owpath, true))
            {
               xml_parsed = true;
               check_pc.attemptCount = ParseContainer.MAX_ATTEMPT;
            }
            else
               check_pc.attemptCount++;

            // close the file
            try
            {
               file_stream.close();
            }
            catch (IOException ioe)
            {
               main.setStatus("Could not close TAGX.000 file");
            }
         }

         // close the path
         owpath.close();
         main.setStatus("Path closed");

         // update the main paths listbox if XML file found
         if (xml_parsed)
         {
            // add the paths to the main window
            main.clearPathList();
            for (int p = 0; p < paths.size(); p++)
               main.addToPathList(((OWPath)paths.get(p)).toString());
         }
      }
      catch (OneWireException e)
      {
         System.out.println(e);
         main.setStatus(e.toString());
      }
      finally
      {
         // end exclusive use of adapter
         adapter.endExclusive();
         main.setStatus("Exclusive 1-Wire aquired released");
      }

      return rslt;
   }

   /**
    * Parse the provided XML input stream with the provided parser.
    * Gather the new TaggedDevices and OWPaths into the global vectors
    * 'taggedDevices' and 'paths'.
    *
    * @param parser parser to parse 1-Wire XML files
    * @param stream  XML file stream
    * @param currentPath  OWPath that was opened to get to this file
    * @param autoSpawnFrames true if new DeviceFrames are spawned with
    *        new taggedDevices discovered
    * @return true an XML file was successfully parsed.
    */
   public static boolean parseStream(TAGParser parser, InputStream stream,
                                     OWPath currentPath,
                                     boolean autoSpawnFrames)
   {
      boolean rslt = false;
      OWPath tempPath;

      try
      {
         // parse the file
         Vector new_devices = parser.parse(stream);

         // get the new paths
         Vector new_paths = parser.getOWPaths();

         main.setStatus("Success, XML parsed with " + new_devices.size() +
                        " devices " + new_paths.size() + " paths");

         // add the new devices to the old list
         for (int i = 0; i < new_devices.size(); i++)
         {
            TaggedDevice current_device = (TaggedDevice) new_devices.get(i);

            // update this devices OWPath depending on where we got it if its OWPath is empty
            tempPath = current_device.getOWPath();
            if (!tempPath.getAllOWPathElements().hasMoreElements())
            {
               // replace this devices path with the current path
               tempPath.copy(currentPath);
               current_device.setOWPath(tempPath);
            }

            // check if spawning frames
            if (autoSpawnFrames)
            {
               if (current_device instanceof TaggedSensor)
               {
                  main.setStatus("Spawning Sensor: " + current_device.getLabel());
                  deviceFrames.addElement(new DeviceMonitorSensor(current_device, logFile));
               }
               else if (current_device instanceof TaggedActuator)
               {
                  main.setStatus("Spawning Actuator: " + current_device.getLabel());
                  deviceFrames.addElement(new DeviceMonitorActuator(current_device, logFile));
               }
            }

            // add the new device to the device list
            taggedDevices.addElement(current_device);
         }

         // add the new paths
         for (int i = 0; i < new_paths.size(); i++)
            paths.addElement(new_paths.get(i));

         rslt = true;

      }
      catch (SAXException se)
      {
         main.setStatus("XML error: " + se);
      }
      catch (IOException ioe)
      {
         main.setStatus("IO error: " + ioe);
      }

      return rslt;
   }

   /**
    * Turn off all branches before doing the first search so devices
    * are not found on the wrong path. (DS2406 and DS2409 specific)
    */
   public static void allBranchesOff()
   {
      byte[] all_lines_off = { (byte)0xCC, (byte)0x66, (byte)0xFF };
      byte[] all_flipflop_off = { (byte)0xCC, (byte)0x55, (byte)0x07,
                                  (byte)0x00, (byte)0x73, (byte)0xFF, (byte)0xFF };
      try
      {
         main.setStatus("Waiting for 1-Wire available");

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         main.setStatus("Exclusive 1-Wire aquired");

         adapter.reset();
         adapter.dataBlock(all_flipflop_off, 0, all_flipflop_off.length);

         main.setStatus("All flip flop off sent");

         adapter.reset();
         adapter.dataBlock(all_lines_off, 0, all_lines_off.length);

         main.setStatus("All lines off sent");
      }
      catch (OneWireException e)
      {
         System.out.println(e);
         main.setStatus(e.toString());
      }
      finally
      {
         // end exclusive use of adapter
         adapter.endExclusive();
         main.setStatus("Exclusive 1-Wire aquired released");
      }
   }
}

