
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
import com.dalsemi.onewire.application.sha.*;
import com.dalsemi.onewire.container.OneWireContainer18;
import com.dalsemi.onewire.container.OneWireContainer33;
import java.util.Vector;
import java.io.*;
import com.dalsemi.onewire.utils.*;
import java.util.*;


public class initrov33
{
   static byte[] page0 =
   {
      ( byte ) 0x0F, ( byte ) 0xAA, ( byte ) 0x00, ( byte ) 0x80,
      ( byte ) 0x03, ( byte ) 0x00, ( byte ) 0x00, ( byte ) 0x00,
      ( byte ) 0x43, ( byte ) 0x4F, ( byte ) 0x50, ( byte ) 0x52,
      ( byte ) 0x00, ( byte ) 0x01, ( byte ) 0x01, ( byte ) 0x00,
      ( byte ) 0x74, ( byte ) 0x9C, ( byte ) 0xFF, ( byte ) 0xFF,
      ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
      ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF,
      ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF, ( byte ) 0xFF
   };

   /**
    * Method printUsageString
    *
    *
    */
   public static void printUsageString ()
   {
      IOHelper.writeLine(
         "SHA iButton Java Demo Transaction Program - 1961S User Initialization.\r\n");
      IOHelper.writeLine("Usage: ");
      IOHelper.writeLine("   java initrov [-pSHA_PROPERTIES_PATH]\r\n");
      IOHelper.writeLine();
      IOHelper.writeLine("If you don't specify a path for the sha.properties file, the ");
      IOHelper.writeLine("current directory and the java lib directory are searched. ");
      IOHelper.writeLine();
      IOHelper.writeLine("Here are examples: ");
      IOHelper.writeLine("   java initrov");
      IOHelper.writeLine("   java initrov -p sha.properties");
      IOHelper.writeLine("   java initrov -p\\java\\lib\\sha.properties");
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
      throws OneWireIOException, OneWireException
   {
      //coprocessor
      long coprID = 0;

      // ------------------------------------------------------------
      // Check for valid path to sha.properties file on the cmd line.
      // ------------------------------------------------------------
      for(int i=0; i<args.length; i++)
      {
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
      copr18.setSpeed(DSPortAdapter.SPEED_REGULAR, false);
      copr18.setSpeedCheck(false);

      // ------------------------------------------------------------
      // Setup the adapter for the coprocessor
      // ------------------------------------------------------------
      DSPortAdapter coprAdapter = null;
      String coprAdapterName = null, coprPort = null;
      try
      {
         coprAdapterName = getProperty("copr.adapter", "{DS9097U}");
         coprPort = getProperty("copr.port", "COM1");

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
         coprAdapter.setSearchAllDevices();
         coprAdapter.reset();
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Error initializing coprocessor adapter");
         e.printStackTrace();
         System.exit(1);
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
      else
      {
         // ---------------------------------------------------------
         // Get the name of the coprocessor service file
         // ---------------------------------------------------------
         String filename = getProperty("copr.filename","COPR.0");

         // ---------------------------------------------------------
         // Check for hardcoded coprocessor address
         // ---------------------------------------------------------
         byte[] coprAddress = getPropertyBytes("copr.address",null);
         long lookupID = 0;
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
            boolean next = coprAdapter.findFirstDevice();
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
                     copr = new SHAiButtonCopr(copr18, filename);

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

      }

      if(copr==null)
      {
         IOHelper.writeLine("No Coprocessor found!");
         System.exit(1);
      }

      IOHelper.writeLine(copr);
      IOHelper.writeLine();


      // ------------------------------------------------------------
      // Create the SHADebit transaction types
      // ------------------------------------------------------------
      //stores DS1963S transaction data
      SHATransaction trans = null;
      String transType = getProperty("ds1961s.transaction.type","Signed");
      if(transType.toLowerCase().indexOf("unsigned")>=0)
      {
         trans = new SHADebitUnsigned(copr,10000,50);
      }
      else
      {
         trans = new SHADebit(copr,10000,50);
      }

      // ------------------------------------------------------------
      // Create the User Buttons objects
      // ------------------------------------------------------------
      //holds DS1963S user buttons
      OneWireContainer33 owc33 = new OneWireContainer33();
      owc33.setSpeed(DSPortAdapter.SPEED_REGULAR, false);
      //owc33.setSpeedCheck(false);

      // ------------------------------------------------------------
      // Get the adapter for the user
      // ------------------------------------------------------------
      DSPortAdapter adapter = null;
      String userAdapterName = null, userPort = null;
      try
      {
         userAdapterName = getProperty("user.adapter","{DS9097U}");
         userPort = getProperty("user.port","COM2");

         if(userPort==null || userAdapterName==null)
         {
            if(coprAdapterName!=null && coprPort!=null)
               adapter = OneWireAccessProvider.getDefaultAdapter();
            else
               adapter = coprAdapter;
         }
         else if(userAdapterName.equals(coprAdapterName) &&
            userPort.equals(coprPort))
         {
            adapter = coprAdapter;
         }
         else
         {
            adapter
               = OneWireAccessProvider.getAdapter(userAdapterName, userPort);
         }

         IOHelper.writeLine("User adapter loaded, adapter: " +
                            adapter.getAdapterName() +
                            " port: " + adapter.getPortName());

         byte[] families = new byte[]{0x33,(byte)0xB3};

         adapter.adapterDetected();
         adapter.targetFamily(families);
         adapter.beginExclusive(false);
         adapter.setSearchAllDevices();
         adapter.reset();
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Error initializing user adapter.");
         e.printStackTrace();
         System.exit(1);
      }

      // ---------------------------------------------------------------
      // Search for the button
      // ---------------------------------------------------------------
      try
      {
         long tmpID = -1;
         boolean next = adapter.findFirstDevice();
         for(; tmpID==-1 && next; next=adapter.findNextDevice())
         {
            tmpID = adapter.getAddressAsLong();
            if(tmpID==coprID)
               tmpID = -1;
            else
               owc33.setupContainer(adapter, tmpID);
         }

         if(tmpID ==-1)
         {
            IOHelper.writeLine("No buttons found!" );
            System.exit(1);
         }
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Adapter error while searching.");
         System.exit(1);
      }

      IOHelper.write("Setting up user button: " );
      IOHelper.writeBytesHex(owc33.getAddress());
      IOHelper.writeLine();

      IOHelper.writeLine(
         "How would you like to enter the authentication secret (unlimited bytes)? ");
      byte[] auth_secret = getBytes(0);
      IOHelper.writeBytes(auth_secret);
      IOHelper.writeLine();

      auth_secret = copr.reformatFor1961S(auth_secret);
      IOHelper.writeLine("Reformatted for compatibility with 1961S buttons");
      IOHelper.writeBytes(auth_secret);
      IOHelper.writeLine("");
      
      IOHelper.writeLine("Initial Balance in Cents? ");
      int initialBalance = IOHelper.readInt(100);
      trans.setParameter(SHADebit.INITIAL_AMOUNT, initialBalance);

      SHAiButtonUser user = new SHAiButtonUser33(copr, owc33,
                                                 true, auth_secret);
      if(trans.setupTransactionData(user))
         IOHelper.writeLine("Transaction data installation succeeded");
      else
         IOHelper.writeLine("Failed to initialize transaction data");

      IOHelper.writeLine(user);
   }

   public static byte[] getBytes(int cnt)
   {
      IOHelper.writeLine("   1 HEX");
      IOHelper.writeLine("   2 ASCII");
      System.out.print("  ? ");
      int choice = IOHelper.readInt(2);

      if(choice==1)
         return IOHelper.readBytesHex(cnt,0x00);
      else
         return IOHelper.readBytesAsc(cnt,0x20);
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
