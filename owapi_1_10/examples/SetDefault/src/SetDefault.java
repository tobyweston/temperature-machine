
/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.io.File;
import java.util.Vector;
import java.util.Properties;
import java.util.Enumeration;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.OneWireAccessProvider;


/**
 * Utility to set the default 1-Wire adapter and port
 *
 * @version    0.01, 15 December 2000
 * @author     DS
 */
public class SetDefault
{

   /**
    * Method main
    *
    *
    * @param args
    *
    */
   public static void main (String args [])
   {
      Properties       onewire_properties = new Properties();
      Vector           adapters           = new Vector(3);
      Vector           ports              = new Vector(3);
      DSPortAdapter    adapter;
      String           default_adapter, default_port, port, path;
      int              num;
      FileOutputStream prop_outfile;
      FileInputStream  prop_infile;
      File             tst_file;
      String           temp_str, key;

      // attempt to open the onewire.properties file
      if (System.getProperty("os.arch").indexOf("TINI") != -1)
         path = "etc" + File.separator;
      else
         path = System.getProperty("java.home") + File.separator + "lib"
                + File.separator;

      // attempt to open the onewire.properties file
      try
      {
         tst_file = new File("onewire.properties");

         if (tst_file.exists())
         {
            System.out.println();
            System.out.println(
               "WARNING, onewire.properties file detected in the current directory.");
            System.out.println(
               "This is NOT the file this application will write but it IS the one");
            System.out.println(
               "read when attempting to open the default adapter.  This application");
            System.out.println("writes the file in " + path);
            System.out.println(
               "To avoid confusion, remove this file from the current directory.");
         }
      }
      catch (Exception e)
      {
         // DRAIN
      }

      // attempt to open the onewire.properties file and print the current defaults
      try
      {
         prop_infile = new FileInputStream(path + "onewire.properties");
         onewire_properties.load(prop_infile);

         System.out.println();
         System.out.println("-----------------------------------------------------------------");
         System.out.println("| Current values in '" + path + "onewire.properties':");
         System.out.println("-----------------------------------------------------------------");
         // enumerate through the properties and display
         for (Enumeration prop_enum = onewire_properties.keys();
                  prop_enum.hasMoreElements(); )
         {
            key = (String)prop_enum.nextElement();
            System.out.println(key + "=" + onewire_properties.getProperty(key));
         }
      }
      catch (Exception e)
      {
         // DRAIN
      }

      // menu header
      System.out.println();
      System.out.println("-----------------------------------------------------------------");
      System.out.println(
            "| Select the new Default Adapter 'onewire.adapter.default'");

      // clarification for drivers under windows (since (2) DS9097U's)
      if ((System.getProperty("os.arch").indexOf("86") != -1)
              && (System.getProperty("os.name").indexOf("Windows") != -1))
         System.out.println("|   {} denote native driver");
      System.out.println("-----------------------------------------------------------------");

      // get the vector of adapters
      for (Enumeration adapter_enum = OneWireAccessProvider.enumerateAllAdapters();
              adapter_enum.hasMoreElements(); )
      {

         // cast the enum as a DSPortAdapter
         adapter = ( DSPortAdapter ) adapter_enum.nextElement();

         adapters.addElement(adapter);
         System.out.println("(" + (adapters.size() - 1) + ") "
                            + adapter.getAdapterName());
         try
         {
            System.out.println("     ver: " + adapter.getAdapterVersion());
            System.out.println("    desc:" + adapter.getPortTypeDescription());
         }
         catch(Exception e){;}
      }

      System.out.println("Enter a number to select the default: ");
      num = getNumber(0, adapters.size() - 1);

      // select the adapter
      adapter         = ( DSPortAdapter ) adapters.elementAt(num);
      default_adapter = adapter.getAdapterName();

      System.out.println();
      System.out.println("-----------------------------------------------------------------");
      System.out.println("| Select the new Default Port 'onewire.port.default' on adapter: "
                         + default_adapter);
      System.out.println("-----------------------------------------------------------------");

      // get the ports
      for (Enumeration port_enum = adapter.getPortNames();
              port_enum.hasMoreElements(); )
      {

         // cast the enum as a String
         port = ( String ) port_enum.nextElement();

         ports.addElement(port);
         System.out.println("(" + (ports.size() - 1) + ") " + port);
      }

      System.out.println("Enter a number to select the default: ");
      num = getNumber(0, ports.size() - 1);

      // select the port
      default_port = ( String ) ports.elementAt(num);

      // set to the properities
      System.out.println();
      System.out.println("Properties object created");
      System.out.println("Attempting to save onewire.properties file");

      // attempt to open the onewire.properties file
      try
      {
         // remove the two properties we are setting
         onewire_properties.remove("onewire.adapter.default");
         onewire_properties.remove("onewire.port.default");

         // add the new properties
         onewire_properties.put("onewire.adapter.default", default_adapter);
         onewire_properties.put("onewire.port.default", default_port);

         // open the file to write
         prop_outfile = new FileOutputStream(path + "onewire.properties");

         // enumerate through the properties and write the new file
         for (Enumeration prop_enum = onewire_properties.keys();
                  prop_enum.hasMoreElements(); )
         {
            key = (String)prop_enum.nextElement();
            temp_str = key + "=" + onewire_properties.getProperty(key) + "\r" + "\n";
            prop_outfile.write(temp_str.getBytes());
         }
      }
      catch (Exception e)
      {
         System.out.println(e);
      }

      System.out.println("onewire.properties file saved to " + path
                         + "onewire.properites");
      System.out.println(
         "Attempting to open the default adapter on the default port");

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         System.out.println("Success!");
      }
      catch (Exception e)
      {
         System.out.println(e);
      }

      System.exit(0);
   }

   /**
    * Retrieve user input from the console.
    *
    * @param min minimum number to accept
    * @param max maximum number to accept
    *
    * @return numberic value entered from the console.
    */
   static int getNumber (int min, int max)
   {
      int     value   = -1;
      boolean fNumber = false;

      while (fNumber == false)
      {
         try
         {
            String str = getString(1);

            value = Integer.parseInt(str);

            if ((value < min) || (value > max))
            {
               System.out.println("Invalid value, range must be " + min
                                  + " to " + max);
               System.out.print("Please enter value again: ");
            }
            else
               fNumber = true;
         }
         catch (NumberFormatException e)
         {
            System.out.println("Invalid Numeric Value: " + e.toString());
            System.out.print("Please enter value again: ");
         }
      }

      return value;
   }

   /**
    * InputStream to read lines
    */
   private static BufferedReader dis =
            new BufferedReader(new InputStreamReader(System.in));

   /**
    * Retrieve user input from the console.
    *
    * @param minLength minumum length of string
    *
    * @return string entered from the console.
    */
   static String getString (int minLength)
   {
      String str;
      boolean done = false;

      try
      {
         do
         {
            str = dis.readLine();
            if(str.length() < minLength)
               System.out.print("String too short try again:");
            else
               done = true;
         }
         while(!done);

         return str;
      }
      catch (java.io.IOException e)
      {
         System.out.println("Error in reading from console: " + e);
      }

      return "";
   }

}
