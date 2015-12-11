
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

import com.dalsemi.onewire.utils.IOHelper;
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.application.sha.*;
import java.util.Vector;
import java.io.*;
import com.dalsemi.onewire.utils.*;
import java.util.*;


public class initcopr
{
   static boolean EMULATE_COPR = false;
   static String SHA_COPR_FILENAME = "sha_copr";

   final static byte[] DEFAULT_COPR_ROMID
      = new byte[]{(byte)0x18,(byte)0xFB,(byte)0x13,0,0,0,0,(byte)0xB2};

   /**
    * Method printUsageString
    *
    *
    */
   public static void printUsageString ()
   {
      System.out.println(
         "SHA iButton Java Demo Transaction Program - Copr Initialization.\r\n");
      System.out.println("Usage: ");
      System.out.println("   java initcopr [-pSHA_PROPERTIES_PATH]\r\n");
      System.out.println();
      System.out.println("If you don't specify a path for the sha.properties file, the ");
      System.out.println("current directory and the java lib directory are searched. ");
      System.out.println();
      System.out.println("Here are examples: ");
      System.out.println("   java initcopr");
      System.out.println("   java initcopr -p sha.properties");
      System.out.println("   java initcopr -p\\java\\lib\\sha.properties");
   }

   /**
    * Method main
    *
    *
    * @param args
    *
    * @throws OneWireException
    * @throws OneWireIOException
    *
    */
   public static void main (String[] args)
      throws OneWireIOException, OneWireException, IOException
   {
      // ------------------------------------------------------------
      // Check for valid path to sha.properties file on the cmd line.
      // ------------------------------------------------------------
      for(int i=0; i<args.length; i++)
      {
         String arg = args[i].toUpperCase();
         if(arg.indexOf("-p")==0)
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
         else
         {
            printUsageString();
            System.exit(1);
         }
      }

      // ------------------------------------------------------------
      // Instantiate coprocessor containers
      // ------------------------------------------------------------
      SHAiButtonCopr copr = null;
      OneWireContainer18 copr18 = new OneWireContainer18();
      copr18.setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
      copr18.setSpeedCheck(false);

      // ------------------------------------------------------------
      // Setup the adapter for the coprocessor
      // ------------------------------------------------------------
      DSPortAdapter coprAdapter = null;
      String coprAdapterName = null, coprPort = null;
      try
      {
         coprAdapterName = getProperty("copr.adapter");
         coprPort = getProperty("copr.port");

         if(coprPort==null || coprAdapterName==null)
         {
            coprAdapter = OneWireAccessProvider.getDefaultAdapter();
         }
         else
         {
            coprAdapter
               = OneWireAccessProvider.getAdapter(coprAdapterName, coprPort);
         }

         IOHelper.writeLine("Coprocessor adapter loaded, adapter: " +
                            coprAdapter.getAdapterName() +
                            " port: " + coprAdapter.getPortName());

         coprAdapter.adapterDetected();
         coprAdapter.targetFamily(0x18);
         coprAdapter.beginExclusive(true);
         coprAdapter.reset();
         coprAdapter.setSearchAllDevices();
         coprAdapter.reset();
         coprAdapter.putByte(0x3c);
         coprAdapter.setSpeed(coprAdapter.SPEED_OVERDRIVE);
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Error initializing coprocessor adapter");
         e.printStackTrace();
         System.exit(1);
      }

      // ---------------------------------------------------------
      // Get the name of the coprocessor service file
      // ---------------------------------------------------------
      String filename = getProperty("copr.filename","COPR.0");

      boolean next = false;
      boolean vmSaveSecrets = true;
      String coprVMfilename = null;

      // ------------------------------------------------------------
      // Find the coprocessor
      // ------------------------------------------------------------
      if(getPropertyBoolean("copr.simulated.isSimulated", false))
      {
         coprVMfilename = getProperty("copr.simulated.filename");
         vmSaveSecrets = getPropertyBoolean("copr.simulated.saveSecrets",true);

         // ---------------------------------------------------------
         // Load emulated coprocessor
         // ---------------------------------------------------------
         System.out.println("Setting up simulated Copressor.");
         System.out.println("Would you like to emulate another coprocessor? (y)");
         if(IOHelper.readLine().toUpperCase().charAt(0)=='Y')
         {
            OneWireContainer18 ibc = new OneWireContainer18();
            ibc.setSpeedCheck(false);
            try
            {
               next = coprAdapter.findFirstDevice();
               while(next && copr==null)
               {
                  try
                  {
                     System.out.println(coprAdapter.getAddressAsLong());
                     ibc.setupContainer(coprAdapter, coprAdapter.getAddressAsLong());
                     copr = new SHAiButtonCopr(ibc, filename);
                  }
                  catch(Exception e){e.printStackTrace();}
                  next = coprAdapter.findNextDevice();
               }
            }
            catch(Exception e){;}
            if(copr==null)
            {
               System.out.println("No coprocessor found to emulate");
               return;
            }

            System.out.println();
            System.out.println("Emulating Coprocessor: "+ibc.getAddressAsString());
            System.out.println();

            //now that we got all that, we need a signing secret and an authentication secret
            System.out.println(
               "How would you like to enter the signing secret (unlimited bytes)? ");
            byte[] sign_secret = getBytes(0);
            IOHelper.writeBytes(sign_secret);

            System.out.println();
            System.out.println(
               "How would you like to enter the authentication secret (unlimited bytes)? ");
            byte[] auth_secret = getBytes(0);
            IOHelper.writeBytes(auth_secret);

            System.out.println();
            if(copr.isDS1961Scompatible())
            {
               //reformat the auth_secret
               auth_secret = SHAiButtonCopr.reformatFor1961S(auth_secret);
               IOHelper.writeBytes(auth_secret);
            }
            System.out.println();
            copr = new SHAiButtonCoprVM(ibc, filename, sign_secret, auth_secret);

            ((SHAiButtonCoprVM)copr).save(coprVMfilename, true);

            System.out.println(copr);
            return;
         }

      }
      else
      {
         // ---------------------------------------------------------
         // Check for hardcoded coprocessor address
         // ---------------------------------------------------------
         byte[] coprAddress = getPropertyBytes("copr.address",null);
         long lookupID = 0, coprID = -1;
         if(coprAddress!=null)
         {
            lookupID = Address.toLong(coprAddress);

            IOHelper.write("Looking for coprocessor: ");
            IOHelper.writeLineHex(lookupID);
         }

         // ---------------------------------------------------------
         // Find hardware coprocessor
         // ---------------------------------------------------------
         try
         {
            next = coprAdapter.findFirstDevice();
            while(copr==null && next)
            {
               try
               {
                  long tmpCoprID = coprAdapter.getAddressAsLong();
                  if(coprAddress==null || tmpCoprID==lookupID)
                  {
                     IOHelper.write("Loading coprocessor file: " + filename +
                                    " from device: ");
                     IOHelper.writeLineHex(tmpCoprID);

                     copr18.setupContainer(coprAdapter, tmpCoprID);

                     //save coprocessor ID
                     coprID = tmpCoprID;
                  }
               }
               catch(Exception e)
               {
                  IOHelper.writeLine(e);
               }

               next = coprAdapter.findNextDevice();
            }
         }
         catch(Exception e)
         {;}

         if(coprID==-1)
         {
            IOHelper.writeLine("No Coprocessor found!");
            System.exit(1);
         }

         System.out.println("Setting up DS1963S as Coprocessor: " +
               Address.toString(copr18.getAddress()));
      }

      // Now that we've got a suitable button for creating a coprocessor,
      // let's ask the user for all the necessary paramters.

      System.out.print(
         "Enter the name of the coprocessor file (usually 'COPR') : ");
      byte[] coprname = IOHelper.readBytesAsc(4,' ');

      System.out.print(
         "Enter the file extension of the coprocessor file (0) : ");
      int coprext = IOHelper.readInt(0);

      System.out.print(
         "Enter the name of the service file (4 characters) : ");
      byte[] name = IOHelper.readBytesAsc(4,' ');

      System.out.print(
         "Enter the file extension of the service file (102 for Money) : ");
      byte ext = (byte)IOHelper.readInt(102);

      System.out.print("Enter authentication page number (7) : ");
      int auth_page = IOHelper.readInt(7);
      if (auth_page < 7)
      {
         System.out.println("Authentication page too low, default to 7");
         auth_page = 7;
      }
      if (auth_page == 8)
      {
         System.out.println("Page already taken, default to 7");
         auth_page = 7;
      }

      System.out.print("Enter workspace page number (9) : ");
      int work_page = IOHelper.readInt(9);
      if (work_page < 7)
      {
         System.out.println("Workspace page too low, default to 9");
         work_page = 9;
      }
      if ((work_page == 8) || (work_page == auth_page))
      {
         System.out.println("Page already taken, default to 9");
         work_page = 9;
      }

      System.out.print("Enter version number (1) : ");
      int  version = IOHelper.readInt(1);

      System.out.println(
         "How would you like to enter the binding data (32 bytes)? ");
      byte[] bind_data = getBytes(32);

      System.out.println(
         "How would you like to enter the binding code (7 bytes)? ");
      byte[] bind_code = getBytes(7);

      // This could be done on the button
      //java.util.Random random = new java.util.Random();
      //random.nextBytes(chlg);
      //  Need to know what the challenge is so that I can reproduce it!
      byte[] chlg = new byte []{0x00,0x00,0x00};

      System.out.println("Enter a human-readable provider name: ");
      String provider_name = IOHelper.readLine();

      System.out.println(
         "Enter an initial signature in HEX (all 0' default): ");
      byte[] sig_ini  = IOHelper.readBytesHex(20, 0);

      System.out.println(
         "Enter any additional text you would like store on the coprocessor: ");
      String aux_data = IOHelper.readLine();

      System.out.println("Enter an encryption code (0): ");
      int enc_code = IOHelper.readInt(0);

      //now that we got all that, we need a signing secret and an authentication secret
      System.out.println(
         "How would you like to enter the signing secret (unlimited bytes)? ");
      byte[] sign_secret = getBytes(0);
      IOHelper.writeBytes(sign_secret);

      System.out.println();
      System.out.println(
         "How would you like to enter the authentication secret (unlimited bytes)? ");
      byte[] auth_secret = getBytes(0);
      IOHelper.writeBytes(auth_secret);

      System.out.println();
      System.out.println(
         "Would you like to reformat the authentication secret for the 1961S? (y or n)");
      String s = IOHelper.readLine();
      if(s.toUpperCase().charAt(0)=='Y')
      {
         //reformat the auth_secret
         auth_secret = SHAiButtonCopr.reformatFor1961S(auth_secret);
         IOHelper.writeLine("authentication secret");
         IOHelper.writeBytes(auth_secret);
         IOHelper.writeLine();
      }

      // signing page must be 8, using secret 0
      int sign_page = 8;

      if(coprVMfilename!=null)
      {
         byte[] RomID = new byte[]
           { (byte)0x18,(byte)0x20,(byte)0xAF,(byte)0x02,
             (byte)0x00,(byte)0x00,(byte)0x00,(byte)0xE7 };
         RomID = getPropertyBytes("copr.simulated.address",RomID);

         copr = new SHAiButtonCoprVM(RomID,
                   sign_page, auth_page, work_page, version, enc_code,
                   ext, name, provider_name.getBytes(), bind_data, bind_code,
                   aux_data.getBytes(), sig_ini, chlg,
                   sign_secret, auth_secret);
         ((SHAiButtonCoprVM)copr).save(coprVMfilename, vmSaveSecrets);
      }
      else
      {
         String coprFilename = new String(coprname) + "." + coprext;
         // initialize this OneWireContainer18 as a valid coprocessor
         copr = new SHAiButtonCopr(copr18, coprFilename, true,
                   sign_page, auth_page, work_page, version, enc_code,
                   ext, name, provider_name.getBytes(), bind_data, bind_code,
                   aux_data.getBytes(), sig_ini, chlg,
                   sign_secret, auth_secret);
      }
      System.out.println("Initialized Coprocessor");
      System.out.println(copr.toString());

      System.out.println("done");
   }

   public static byte[] getBytes(int cnt)
   {
      System.out.println("   1 HEX");
      System.out.println("   2 ASCII");
      System.out.print("  ? ");
      int choice = IOHelper.readInt(2);

      if(choice==1)
         return IOHelper.readBytesHex(cnt,0x00);
      else
         return IOHelper.readBytesAsc(cnt,0x00);
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
