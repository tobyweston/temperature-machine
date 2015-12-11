
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

import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.application.file.OWFileOutputStream;
import com.dalsemi.onewire.application.file.OWFileNotFoundException;
import com.dalsemi.onewire.utils.OWPath;
import java.util.*;
import java.io.*;
import java.lang.InterruptedException;
import javax.swing.*;

/**
 * Main class for a SWING based application that prompts to create
 * a 1-Wire XML tag.  It can create (4) kinds of sensors, (2) kinds
 * of actuators or a branch XML tag.  The tag can be saved on the
 * computer system or to the 1-Wire File system.  The default 1-Wire
 * adapter is used.
 *
 * TODO:
 *    1. Create an ImageIcon to use in all of the dialogs
 *    2. Use a parent window?
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class TagCreator
{
   /**
    * Method main, that provides the dialog box prompts to create the
    * 1-Wire XML tag.
    *
    * @param args command line arguments
    */
   public static void main(String[] args)
   {
      OneWireContainer tag_owd;
      DSPortAdapter adapter = null;
      Vector owd_vect = new Vector(5);
      boolean get_min=false, get_max=false, get_channel=false,
        get_init=false, get_scale=false;
      String file_type, label, tag_type, method_type=null, cluster;
      String min=null,max=null,channel=null,init=null,scale=null;

      // connect now message
      JOptionPane.showMessageDialog(null,
         "Connect the 1-Wire device to Tag onto the Default 1-Wire port",
         "1-Wire Tag Creator",
         JOptionPane.INFORMATION_MESSAGE);

      try
      {
         // get the default adapter
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // find all parts
         owd_vect = findAllDevices(adapter);

         // select a device
         tag_owd = selectDevice(owd_vect,"Select the 1-Wire Device to Tag");

         // enter the label for this devcie
         label = JOptionPane.showInputDialog(null,
            "Enter a human readable label for this device: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (label == null)
            throw new InterruptedException("Aborted");

         // enter the cluster for this devcie
         cluster = JOptionPane.showInputDialog(null,
            "Enter a cluster where this device will reside: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (cluster == null)
            throw new InterruptedException("Aborted");

         // select the type of device
         String[] tag_types = { "sensor", "actuator", "branch" };
         tag_type = (String)JOptionPane.showInputDialog(null,
            "Select the Tag Type", "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE, null,
            tag_types, tag_types[0]);
         if (tag_type == null)
            throw new InterruptedException("Aborted");

         // check if branch selected
         if (tag_type == "branch")
         {
            get_init = true;
            get_channel = true;
         }
         // sensor
         else if (tag_type == "sensor")
         {
            String[] sensor_types = { "Contact", "Humidity", "Event", "Thermal" };
            method_type = (String)JOptionPane.showInputDialog(null,
               "Select the Sensor Type", "1-Wire tag Creator",
               JOptionPane.INFORMATION_MESSAGE, null,
               sensor_types, sensor_types[0]);
            if (method_type == null)
               throw new InterruptedException("Aborted");

            // contact
            if (method_type == "Contact")
            {
               get_min = true;
               get_max = true;
            }
            // Event
            else if (method_type == "Event")
            {
               get_channel = true;
               get_max = true;
            }
         }
         // actuator
         else
         {
            String[] actuator_types = { "Switch", "D2A" };
            method_type = (String)JOptionPane.showInputDialog(null,
               "Select the Actuator Type", "1-Wire tag Creator",
               JOptionPane.INFORMATION_MESSAGE, null,
               actuator_types, actuator_types[0]);
            if (method_type == null)
               throw new InterruptedException("Aborted");

            get_channel = true;
            get_init = true;
            get_min = true;
            get_max = true;
         }

         // enter the tags required
         if (get_min)
            min = JOptionPane.showInputDialog(null,
            "Enter the 'min' value: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (min == null)
            get_min = false;

         if (get_max)
            max = JOptionPane.showInputDialog(null,
            "Enter the 'max' value: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (max == null)
            get_max = false;

         if (get_channel)
            channel = JOptionPane.showInputDialog(null,
            "Enter the 'channel' value: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (channel == null)
            get_channel = false;

         if (get_init)
            init = JOptionPane.showInputDialog(null,
            "Enter the 'init' value: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (init == null)
            get_init = false;

         if (get_scale)
            scale = JOptionPane.showInputDialog(null,
            "Enter the 'scale' value: ",
            "1-Wire tag Creator",
            JOptionPane.INFORMATION_MESSAGE);
         if (scale == null)
            get_scale = false;

         // build the XML file
         Vector xml = new Vector(5);
         xml.addElement("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
         xml.addElement("<cluster name=\"" + cluster + "\">");
         xml.addElement(" <" + tag_type + " addr=\"" + tag_owd.getAddressAsString() +
                        "\" type=\"" + method_type + "\">");
         xml.addElement("  <label>" + label + "</label>");
         if (get_max)
            xml.addElement("  <max>" + max + "</max>");
         if (get_min)
            xml.addElement("  <min>" + min + "</min>");
         if (get_channel)
            xml.addElement("  <channel>" + channel + "</channel>");
         if (get_init)
            xml.addElement("  <init>" + init + "</init>");
         if (get_scale)
            xml.addElement("  <scale>" + scale + "</scale>");
         xml.addElement(" </" + tag_type + ">");
         xml.addElement("</cluster>");

         // display the XML file
         JList list = new JList(xml.toArray());
         if (JOptionPane.showConfirmDialog(null,
            list, "Is this correct?",
            JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
            throw new InterruptedException("Aborted");

         // loop until file written
         boolean file_written = false;
         do
         {
            // Check if doing desktop or 1-Wire file
            String[] file_types = { "Desktop File", "1-Wire File"};
            file_type = (String)JOptionPane.showInputDialog(null,
               "Select where to put this XML 1-Wire Tag file", "1-Wire tag Creator",
               JOptionPane.INFORMATION_MESSAGE, null,
               file_types, file_types[0]);
            if (file_type == null)
               throw new InterruptedException("Aborted");

            // save to a PC file
            if (file_type == "Desktop File")
            {
               JFileChooser chooser = new JFileChooser();
               int returnVal = chooser.showSaveDialog(null);
               if (returnVal == JFileChooser.APPROVE_OPTION)
               {
                  try
                  {
                     PrintWriter writer = new PrintWriter(
                         new FileOutputStream(chooser.getSelectedFile().getCanonicalPath(), true));
                     for (int i = 0; i < xml.size(); i++)
                        writer.println(xml.elementAt(i));
                     writer.flush();
                     writer.close();
                     JOptionPane.showMessageDialog(null,
                        "XML File saved to: " + chooser.getSelectedFile().getCanonicalPath(),
                        "1-Wire Tag Creator",
                        JOptionPane.INFORMATION_MESSAGE);
                     file_written = true;
                  }
                  catch (FileNotFoundException e)
                  {
                     System.out.println(e);
                     JOptionPane.showMessageDialog(null,
                        "ERROR saving XML File: " + chooser.getSelectedFile().getCanonicalPath(),
                        "1-Wire Tag Creator",
                        JOptionPane.WARNING_MESSAGE);
                  }
               }
            }
            // 1-Wire file
            else
            {
               // search parts again in case the target device was just connected
               owd_vect = findAllDevices(adapter);

               // select the 1-Wire device to save the file to
               tag_owd = selectDevice(owd_vect,"Select the 1-Wire Device to place XML Tag");

               // attempt to write to the filesystem of this device
               try
               {
                  PrintWriter writer = new PrintWriter(
                     new OWFileOutputStream(tag_owd,"TAGX.0"));
                  for (int i = 0; i < xml.size(); i++)
                     writer.println(xml.elementAt(i));
                  writer.flush();
                  writer.close();
                  JOptionPane.showMessageDialog(null,
                     "XML File saved to: " + tag_owd.getAddressAsString() + "\\TAGX.000",
                     "1-Wire Tag Creator",
                     JOptionPane.INFORMATION_MESSAGE);
                     file_written = true;
               }
               catch (OWFileNotFoundException e)
               {
                  System.out.println(e);
                  JOptionPane.showMessageDialog(null,
                     "ERROR saving XML File: " + tag_owd.getAddressAsString() + "\\TAGX.000",
                     "1-Wire Tag Creator",
                     JOptionPane.WARNING_MESSAGE);
               }
            }

            // check if file not written
            if (!file_written)
            {
               if (JOptionPane.showConfirmDialog(null,
                  "Try to save file again?", "1-Wire Tag Creator",
                  JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION)
                  throw new InterruptedException("Aborted");
            }
         }
         while (!file_written);
      }
      catch(Exception e)
      {
         System.out.println(e);
      }
      finally
      {
         if (adapter != null)
         {
            // end exclusive use of adapter
            adapter.endExclusive();

            // free the port used by the adapter
            System.out.println("Releasing adapter port");
            try
            {
               adapter.freePort();
            }
            catch (OneWireException e)
            {
               System.out.println(e);
            }
         }
      }

      System.exit(0);
   }

   /**
    * Search for all devices on the provided adapter and return
    * a vector
    *
    * @param  adapter valid 1-Wire adapter
    *
    * @return Vector or OneWireContainers
    */
   public static Vector findAllDevices(DSPortAdapter adapter)
   {
      Vector owd_vect = new Vector(3);
      OneWireContainer owd;

      try
      {
         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

         // enumerate through all the 1-Wire devices and collect them in a vector
         for(Enumeration owd_enum = adapter.getAllDeviceContainers();
             owd_enum.hasMoreElements(); )
         {
            owd = (OneWireContainer)owd_enum.nextElement();
            owd_vect.addElement(owd);
         }
      }
      catch(Exception e)
      {
         System.out.println(e);
      }

      return owd_vect;
   }

   /**
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    *
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer selectDevice(Vector owd_vect, String title)
      throws InterruptedException
   {
      // create a menu
      Vector menu = new Vector(owd_vect.size());
      String temp_str;
      OneWireContainer owd;
      int i;

      for (i = 0; i < owd_vect.size(); i++)
      {
         owd = (OneWireContainer)owd_vect.elementAt(i);
         temp_str = new String(owd.getAddressAsString() +
                  " - " + owd.getName());
         if (owd.getAlternateNames().length() > 0)
            temp_str += "/" + owd.getAlternateNames();

         menu.addElement(temp_str);
      }

      String selectedValue = (String)JOptionPane.showInputDialog(null,
         title, "1-Wire Tag Creator",
         JOptionPane.INFORMATION_MESSAGE, null,
         menu.toArray(), menu.toArray()[0]);

      if (selectedValue != null)
         return (OneWireContainer)owd_vect.elementAt(menu.indexOf(selectedValue));
      else
         throw new InterruptedException("Quit in device selection");
   }
}

