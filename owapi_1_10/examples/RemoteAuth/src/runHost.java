/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.io.*;
import java.net.*;
import java.util.*;

import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.application.sha.*;

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.utils.*;


/**
 * Starts the host component for NetAdapter clients on the local machine.
 * If no options are specified, the default adapter for this machine is used
 * and the host is launched as a multi-threaded server using the defaults.
 *
 */
public class runHost
{
   static final String strUsage =
"Starts the host component for NetAdapter clients on the local machine.\n" +
"If no options are specified, the default adapter for this machine is used\n" +
"and the host is launched as a multi-threaded server using the defaults:\n" +
"\n" +
"  Host Listen Port: " + AuthenticationConstants.DEFAULT_PORT + "\n" +
"\n" +
"syntax: java AuthHost <options>\n" +
"\n" +
"Options:\n" +
"  -props                    Pulls all defaults from the onewire.properties\n" +
"                            file rather than using the defaults set in\n" +
"                            com.dalsemi.onewire.adapter.NetAdapterConstants.\n" +
"  -p      PATH              This is the path to the SHA properties file.\n" +
"  -listenPort NUM           Sets the host's listening port for incoming\n" +
"                            socket connections.\n" +
"  -secret STRING            Sets the shared secret for authenticating incoming\n" +
"                            client connections.\n";

   public static void usage()
   {
      System.out.println();
      System.out.println(strUsage);
      System.exit(1);
   }

   public static void main(String[] args)
      throws Exception
   {
      String adapterName = null, adapterPort = null;
      int listenPort = AuthenticationConstants.DEFAULT_PORT;
      boolean multithread = true;
      String secret = AuthenticationConstants.DEFAULT_SECRET;
      byte[] SNum = new byte[8];
      byte[] data = new byte[7];

      //coprocessor
      long coprID = 0;
     	SHAiButtonCopr copr = null;

      boolean useProperties = false;

     	// ------------------------------------------------------------
     	// Check for valid path to sha.properties file on the cmd line.
     	// ------------------------------------------------------------
     	for(int i=0; i<args.length; i++)
     	{
        	char c = args[0].charAt(1);
        	String arg = args[i].toUpperCase();
        	if(arg.indexOf("-P")==0)
        	{
           	String sha_props_path;
           	if(arg.length()==2)
              	sha_props_path = args[++i];
           	else
              	sha_props_path = arg.substring(2);

           	// attempt to open the sha.properties file
           	try
           	{
              	FileInputStream prop_file
                 	= new FileInputStream(sha_props_path
                                       	+ "sha.properties");
              	sha_properties = new Properties();
              	sha_properties.load(prop_file);
           	}
           	catch(Exception e)
           	{
              	sha_properties = null;
           	}
        	}
        	else if(arg.equalsIgnoreCase("-listenPort"))
         {
            listenPort = Integer.parseInt(args[++i]);
         }
         else if(arg.equalsIgnoreCase("-secret"))
         {
            secret = args[++i];
         }
         else if(args[0].charAt(0)!='-' || c=='h' || c=='H' || c=='?')
         {
            System.out.println("Invalid option: " + arg);
            usage();
         }
     	}

      // ------------------------------------------------------------
      // Find the coprocessor
      // ------------------------------------------------------------
      if(getPropertyBoolean("copr.simulated.isSimulated", false))
      {
         String coprVMfilename = getProperty("copr.simulated.filename");
         // ---------------------------------------------------------
         // Load emulated coprocessor
         // ---------------------------------------------------------
         try
         {
            copr = new SHAiButtonCoprVM(coprVMfilename);
         }
         catch(Exception e)
         {
            IOHelper.writeLine("Invalid Coprocessor Data File");
            e.printStackTrace();
            System.exit(1);
         }
      }

      if(copr==null)
      {
         IOHelper.writeLine("No Coprocessor found!");
         System.exit(1);
      }

      IOHelper.writeLine(copr);
      IOHelper.writeLine();

 		AuthenticationHost sh = new AuthenticationHost(copr,listenPort,
  		                                               secret);

      sh.run();
   }

   static Properties sha_properties = null;
   /**
    * Gets the specfied onewire property.
    * Looks for the property in the following locations:
    * <p>
    * <ul>
    * <li> In System.properties
    * <li> In onewire.properties file in current directory
    *      or < java.home >/lib/ (Desktop) or /etc/ (TINI)
    * <li> 'smart' default if property is 'onewire.adapter.default'
    *      or 'onewire.port.default'
    * </ul>
    *
    * @param propName string name of the property to read
    *
    * @return  <code>String</code> representing the property value or <code>null</code> if
    *          it could not be found (<code>onewire.adapter.default</code> and
    *          <code>onewire.port.default</code> may
    *          return a 'smart' default even if property not present)
    */
   public static String getProperty (String propName)
   {
      // first, try system properties
      try
      {
         String ret_str = System.getProperty(propName, null);
         if(ret_str!=null)
            return ret_str;
      }
      catch (Exception e)
      { ; }

      // if defaults not found then try sha.properties file
      if(sha_properties==null)
      {
         //try to load sha_propreties file
         FileInputStream prop_file = null;

         // loop to attempt to open the sha.properties file in two locations
         // .\sha.properties or <java.home>\lib\sha.properties
         String path = "";

         for (int i = 0; i <= 1; i++)
         {

            // attempt to open the sha.properties file
            try
            {
               prop_file = new FileInputStream(path + "sha.properties");
               // attempt to read the onewire.properties
               try
               {
                  sha_properties = new Properties();
                  sha_properties.load(prop_file);
               }
               catch (Exception e)
               {
                  //so we remember that it failed
                  sha_properties = null;
               }
            }
            catch (IOException e)
            {
               prop_file = null;
            }

            // check to see if we now have the properties loaded
            if (sha_properties != null)
               break;

            // try the second path
            path = System.getProperty("java.home") + File.separator + "lib"
                   + File.separator;
         }
      }

      if(sha_properties==null)
      {
         IOHelper.writeLine("Can't find sha.properties file");
         return null;
      }
      else
      {
         Object ret = sha_properties.get(propName);
         if(ret==null)
            return null;
         else
            return ret.toString();
      }
   }

   public static String getProperty (String propName, String defValue)
   {
      String ret = getProperty(propName);
      return (ret==null) ? defValue : ret;
   }

   public static boolean getPropertyBoolean(String propName, boolean defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
         defValue = Boolean.valueOf(strValue).booleanValue();
      return defValue;
   }


   public static byte[] getPropertyBytes(String propName, byte[] defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
      {
         //only supports up to 128 bytes of data
         byte[] tmp = new byte[128];

         //split the string on commas and spaces
         StringTokenizer strtok = new StringTokenizer(strValue,", ");

         //how many bytes we got
         int i = 0;
         while(strtok.hasMoreElements())
         {
            //this string could have more than one byte in it
            String multiByteStr = strtok.nextToken();
            int strLen = multiByteStr.length();

            for(int j=0; j<strLen; j+=2)
            {
               //get just two nibbles at a time
               String byteStr
                  = multiByteStr.substring(j, Math.min(j+2, strLen));

               long lng = 0;
               try
               {
                  //parse the two nibbles into a byte
                  lng = Long.parseLong(byteStr, 16);
               }
               catch(NumberFormatException nfe)
               {
                  nfe.printStackTrace();

                  //no mercy!
                  return defValue;
               }

               //store the byte and increment the counter
               if(i<tmp.length)
                  tmp[i++] = (byte)(lng&0x0FF);
            }
         }
         if(i>0)
         {
            byte[] retVal = new byte[i];
            System.arraycopy(tmp, 0, retVal, 0, i);
            return retVal;
         }
      }
      return defValue;
   }

}





