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

import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.application.sha.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.*;
import java.util.*;
import java.io.*;

/**
 * Title:        SHA Debit Demo
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      Maxim/Dallas Semiconductor
 * @author SKH
 * @version 1.0
 */
public class SHADebitDemo
{

   /** turns on DEBUG messages */
   static final boolean DEBUG = false;

   //not supported on TINI
   //static final java.text.NumberFormat nf
   //      = java.text.NumberFormat.getCurrencyInstance();


   /**
    * Method printUsageString
    *
    *
    */
   public static void printUsageString ()
   {
      IOHelper.writeLine(
         "SHA iButton Java Demo Transaction Program.\r\n");
      IOHelper.writeLine("Usage: ");
      IOHelper.writeLine("   java SHADebitDemo [-pSHA_PROPERTIES_PATH]\r\n");
      IOHelper.writeLine();
      IOHelper.writeLine("If you don't specify a path for the sha.properties file, the ");
      IOHelper.writeLine("current directory and the java lib directory are searched. ");
      IOHelper.writeLine();
      IOHelper.writeLine("Here are examples: ");
      IOHelper.writeLine("   java SHADebitDemo");
      IOHelper.writeLine("   java SHADebitDemo -p sha.properties");
      IOHelper.writeLine("   java SHADebitDemo -p\\java\\lib\\sha.properties");
   }

   public static void main(String[] args)
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
      //stores DS1963S transaction type
      SHATransaction debit18 = null;

      String transType18 = getProperty("transaction.type", "signed");
      transType18 = transType18.toLowerCase();
      if(transType18.equals("signed"))
      {
         debit18 = new SHADebit(copr,10000,50);
      }
      else
      {
         debit18 = new SHADebitUnsigned(copr,10000,50);
      }

      //stores DS1961S transaction type
      SHATransaction debit33 = null;

      String transType33 = getProperty("ds1961s.transaction.type", "unsigned");
      transType33 = transType33.toLowerCase();
      if(transType33.equals(transType18))
      {
         debit33 = debit18;
      }
      else if(transType33.indexOf("unsigned")>=0)
      {
         //everything is seemless if you use the authentication secret
         //as the button's secret.
         debit33 = new SHADebitUnsigned(copr,10000,50);
      }
      else
      {
         //if the 1961S uses the authentication secret,
         //you'll need another button for generating the
         //write authorization MAC, but you can share the
         //1963S debit code for signed data.
         debit33 = new SHADebit(copr,10000,50);
      }

      //Transaction super class, swap variable
      SHATransaction trans = null;

      // ------------------------------------------------------------
      // Get the write-authorization coprocessor for ds1961S
      // ------------------------------------------------------------
      //first get the write authorization adapter
      DSPortAdapter authAdapter = null;
      String authAdapterName = null, authPort = null;
      try
      {
         authAdapterName = getProperty("ds1961s.copr.adapter");
         authPort = getProperty("ds1961s.copr.port");

         if(authAdapterName==null || authAdapterName==null)
         {
            if(coprAdapterName!=null && coprPort!=null)
               authAdapter = OneWireAccessProvider.getDefaultAdapter();
            else
               authAdapter = coprAdapter;
         }
         else if(coprAdapterName.equals(authAdapterName) &&
                 coprPort.equals(authPort))
         {
            authAdapter = coprAdapter;
         }
         else
         {
            authAdapter
               = OneWireAccessProvider.getAdapter(authAdapterName, authPort);
         }

         IOHelper.writeLine("Write-Authorization adapter loaded, adapter: " +
                            authAdapter.getAdapterName() +
                            " port: " + authAdapter.getPortName());

         byte[] families = new byte[]{0x18};

         authAdapter.adapterDetected();
         authAdapter.targetFamily(families);
         authAdapter.beginExclusive(false);
         authAdapter.reset();
         authAdapter.setSearchAllDevices();
         authAdapter.reset();
         authAdapter.putByte(0x3c);
         authAdapter.setSpeed(DSPortAdapter.SPEED_OVERDRIVE);
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Error initializing write-authorization adapter.");
         e.printStackTrace();
         System.exit(1);
      }

      //now find the coprocessor
      SHAiButtonCopr authCopr = null;

      // -----------------------------------------------------------
      // Check for hardcoded write-authorizaton coprocessor address
      // -----------------------------------------------------------
      byte[] authCoprAddress = getPropertyBytes("ds1961s.copr.address",null);
      long lookupID = 0;
      if(authCoprAddress!=null)
      {
         lookupID = Address.toLong(authCoprAddress);

         IOHelper.write("Looking for coprocessor: ");
         IOHelper.writeLineHex(lookupID);
      }
      if(lookupID==coprID)
      {
         //it's the same as the standard coprocessor.
         //valid only if we're not signing the data
         authCopr = copr;
      }
      else
      {
         // ---------------------------------------------------------
         // Find write-authorization coprocessor
         // ---------------------------------------------------------
         try
         {
            String filename = getProperty("ds1961s.copr.filename","COPR.1");
            OneWireContainer18 auth18 = new OneWireContainer18();
            auth18.setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
            auth18.setSpeedCheck(false);

            boolean next = authAdapter.findFirstDevice();
            while(authCopr==null && next)
            {
               try
               {
                  long tmpAuthID = authAdapter.getAddressAsLong();
                  if(authCoprAddress==null || tmpAuthID==lookupID)
                  {
                     IOHelper.write("Loading coprocessor file: " + filename +
                                    " from device: ");
                     IOHelper.writeLineHex(tmpAuthID);

                     auth18.setupContainer(authAdapter, tmpAuthID);
                     authCopr = new SHAiButtonCopr(auth18, filename);
                  }
               }
               catch(Exception e)
               {
                  IOHelper.writeLine(e);
               }

               next = authAdapter.findNextDevice();
            }
         }
         catch(Exception e)
         {
            IOHelper.writeLine(e);
         }
         if(authCopr==null)
         {
            IOHelper.writeLine("no write-authorization coprocessor found");
            if(copr instanceof SHAiButtonCoprVM)
            {
               authCopr = copr;
               IOHelper.writeLine("Re-using SHAiButtonCoprVM");
            }
         }
      }

      IOHelper.writeLine(authCopr);
      IOHelper.writeLine();

      // ------------------------------------------------------------
      // Create the User Buttons objects
      // ------------------------------------------------------------
      //holds DS1963S user buttons
      SHAiButtonUser18 user18 = new SHAiButtonUser18(copr);
      OneWireContainer18 owc18 = new OneWireContainer18();
      owc18.setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
      owc18.setSpeedCheck(false);

      //holds DS1961S user buttons
      SHAiButtonUser33 user33 = new SHAiButtonUser33(copr, authCopr);
      OneWireContainer33 owc33 = new OneWireContainer33();
      owc33.setSpeed(DSPortAdapter.SPEED_OVERDRIVE, false);
      //owc33.setSpeedCheck(false);

      //Holds generic user type, swap variable
      SHAiButtonUser user = null;

      // ------------------------------------------------------------
      // Get the adapter for the user
      // ------------------------------------------------------------
      DSPortAdapter adapter = null;
      String userAdapterName = null, userPort = null;
      try
      {
         userAdapterName = getProperty("user.adapter");
         userPort = getProperty("user.port");

         if(userPort==null || userAdapterName==null)
         {
            if(coprAdapterName!=null && coprPort!=null)
            {
               if(authAdapterName!=null && authPort!=null)
                  adapter = OneWireAccessProvider.getDefaultAdapter();
               else
                  adapter = authAdapter;
            }
            else
               adapter = coprAdapter;
         }
         else if(userAdapterName.equals(authAdapterName) &&
            userPort.equals(authPort))
         {
            adapter = authAdapter;
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

         byte[] families = new byte[]{0x18,0x33};
         families = getPropertyBytes("transaction.familyCodes", families);
         IOHelper.write("Supporting the following family codes: ");
         IOHelper.writeBytesHex(" ", families, 0, families.length);

         adapter.adapterDetected();
         adapter.targetFamily(families);
         adapter.beginExclusive(true);
         adapter.reset();
         adapter.setSearchAllDevices();
         adapter.reset();
         adapter.putByte(0x3c);
         adapter.setSpeed(DSPortAdapter.SPEED_OVERDRIVE);
      }
      catch(Exception e)
      {
         IOHelper.writeLine("Error initializing user adapter.");
         e.printStackTrace();
         System.exit(1);
      }

      //timing variables
      long t0=0, t1=0, t2=0, t3=0, t4=0, t5=0;

      //address of current device
      final byte[] address = new byte[8];

      //result of findNextDevice/findFirstDevice
      boolean next = false;

      //holds list of known buttons
      long[] buttons = new long[16];
      //count of how many buttons are in buttons array
      int index = 0;

      //temporary id representing current button
      long tmpID = -1;
      //array of buttons looked at during this search
      long[] temp = new long[16];
      //count of how many buttons in temp array
      int cIndex = 0;

      //flag indiciating whether or not temp array represents
      //the complete list of buttons on the network.
      boolean wholeList = false;

      System.out.println();
      System.out.println();
      System.out.println("**********************************************************");
      System.out.println("   Beginning The Main Application Loop (Search & Debit)");
      System.out.println("           Press Enter to Exit Application");
      System.out.println("**********************************************************");
      System.out.println();

      //application infinite loop
      for(boolean applicationFinished = false; !applicationFinished;)
      {
         try
         {
            if(coprAdapter!=adapter)
            {
               //in case coprocessor communication got hosed, make sure
               //the coprocessor adapter is in overdrive
               coprAdapter.setSpeed(adapter.SPEED_REGULAR);
               coprAdapter.reset();
               coprAdapter.putByte(0x3c); //overdrive skip rom
               coprAdapter.setSpeed(adapter.SPEED_OVERDRIVE);
            }
            if(authAdapter!=coprAdapter && authAdapter!=adapter)
            {
               //in case coprocessor communication with the write-
               //authorization coprocessor got hosed, make sure
               //the coprocessor adapter is in overdrive
               authAdapter.setSpeed(adapter.SPEED_REGULAR);
               authAdapter.reset();
               authAdapter.putByte(0x3c); //overdrive skip rom
               authAdapter.setSpeed(adapter.SPEED_OVERDRIVE);
            }
         }
         catch(Exception e){;}

         // ---------------------------------------------------------
         // Search for new buttons
         // ---------------------------------------------------------
         boolean buttonSearch = true;

         //Button search loop, waits forever until new button appears.
         while(buttonSearch && !applicationFinished)
         {
            wholeList = false;
            t0 = System.currentTimeMillis();
            try
            {
               //Go into overdrive
               adapter.setSpeed(adapter.SPEED_REGULAR);
               adapter.reset();
               adapter.putByte(0x3c); //overdrive skip rom
               adapter.setSpeed(adapter.SPEED_OVERDRIVE);

               // Begin search for new buttons
               if(!next)
               {
                  wholeList = true;
                  next = adapter.findFirstDevice();
               }

               for(tmpID=-1, cIndex=0;
                        next && (tmpID==-1);
                              next=adapter.findNextDevice())
               {
                  tmpID = adapter.getAddressAsLong();
                  if(tmpID!=coprID)
                  {
                     temp[cIndex++] = tmpID;
                     for(int i=0; i<index; i++)
                     {
                        if(buttons[i] == tmpID)
                        {//been here all along
                           tmpID = -1;
                           i = index;
                        }
                     }

                     if(tmpID!=-1)
                     {
                        //populate address array
                        adapter.getAddress(address);
                     }
                  }
                  else
                     tmpID = -1;
               }

               //if we found a new button
               if(tmpID!=-1)
               {
                  //add it to the main list
                  buttons[index++] = tmpID;

                  //quite searching, we got one!
                  buttonSearch = false;
               }
               else if(wholeList)
               {
                  //went through whole list with nothing new
                  //update the main list of buttons
                  buttons = temp;
                  index = cIndex;

                  //might as well play nice, every once in a while
                  Thread.yield();

                  //if user presses the enter key, we'll quit and clean up nicely
                  applicationFinished = (System.in.available()>0);
               }
            }
            catch(Exception e)
            {
               if(DEBUG)
               {
                  IOHelper.writeLine("adapter hiccup while searching");
                  e.printStackTrace();
               }
            }
         }

         if(applicationFinished)
            continue;

         // ---------------------------------------------------------
         // Perform the transaction
         // ---------------------------------------------------------
         try
         {
            t1 = System.currentTimeMillis();

            //de-ref the user
            user = null;

            //check for button family code
            if((tmpID&0x18)==(byte)0x18)
            {
               //get transactions for ds1963s
               trans = debit18;
               owc18.setupContainer(adapter, address);
               if(user18.setiButton18(owc18))
               {
                  user = user18;
               }
            }
            else if((tmpID&0x33)==(byte)0x33)
            {
               //get transactions for 1961S
               trans = debit33;
               owc33.setupContainer(adapter, address);
               if(user33.setiButton33(owc33))
               {
                  user = user33;
               }
            }

            if(user!=null)
            {
               System.out.println();
               System.out.println(user.toString());
               t2 = System.currentTimeMillis();
               if(trans.verifyUser(user))
               {
                  t3 = System.currentTimeMillis();
                  if(trans.verifyTransactionData(user))
                  {
                     t4 = System.currentTimeMillis();
                     if(!trans.executeTransaction(user, true))
                        System.out.println("Execute Transaction Failed");
                     t5 = System.currentTimeMillis();

                     System.out.println("  Debit Amount: $00.50");
                     System.out.print("User's balance: $");
                     int balance = trans.getParameter(SHADebit.USER_BALANCE);
                     System.out.println(Convert.toString(balance/100d, 2));

                  }
                  else
                     System.out.println("Verify Transaction Data Failed");
               }
               else
                  System.out.println("Verify User Authentication Failed");
            }
            else
               System.out.println("Not a SHA user of this service");

            System.out.print("Total time: ");
            System.out.println(t5-t0);
            System.out.print("Executing transaction took: ");
            System.out.println(t5-t4);
            System.out.print("Verifying data took: ");
            System.out.println(t4-t3);
            System.out.print("Verifying user took: ");
            System.out.println(t3-t2);
            System.out.print("Loading user data took: ");
            System.out.println(t2-t1);
            System.out.print("Finding user took: ");
            System.out.println(t1-t0);

            //report all errors
            if(trans.getLastError()!=0)
            {
               IOHelper.writeLine("Last Error Code: ");
               IOHelper.writeLine(trans.getLastError());
               if(trans.getLastError()==trans.COPROCESSOR_FAILURE)
               {
                  IOHelper.writeLine("COPR Error Code: ");
                  IOHelper.writeLine(copr.getLastError());
               }
            }
         }
         catch(Exception e)
         {
            System.err.println("Transaction failed!");
            e.printStackTrace();
         }
      }

      // --------------------------------------------------------------
      // Some Friendly Cleanup
      // --------------------------------------------------------------
      adapter.endExclusive();
      coprAdapter.endExclusive();
      authAdapter.endExclusive();
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