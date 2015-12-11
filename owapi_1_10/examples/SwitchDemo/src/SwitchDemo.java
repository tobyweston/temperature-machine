/*---------------------------------------------------------------------------
 * Copyright (C) 2000 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.util.*;
import java.io.*;
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;
import java.lang.InterruptedException;

/** 
 * Console application to View and change the state of a Switch
 *
 * History:
 *
 * @version    0.00, Oct 11 2000
 * @author     DS
 */	
public class SwitchDemo
{
   /** 
    * Main for 1-Wire Memory utility
    */
   public static void main(String args[]) 
   {
      Vector owd_vect = new Vector(5);
      SwitchContainer sw = null;
      int ch;
      boolean done = false;
      DSPortAdapter adapter = null; 
      byte[] state;

      System.out.println();
      System.out.println("1-Wire Switch utility console application: Version 0.00");
      // os info
      System.out.print("Arch: " + System.getProperty("os.arch"));
      System.out.print(",  OS Name: " + System.getProperty("os.name"));
      System.out.println(",  OS Version: " + System.getProperty("os.version"));
      System.out.println();

      try
      {     
         // get the default adapter  
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // adapter driver info
         System.out.println("=========================================================================");
         System.out.println("== Adapter Name: " + adapter.getAdapterName());
         System.out.println("== Adapter Port description: " + adapter.getPortTypeDescription());
         System.out.println("== Adapter Version: " + adapter.getAdapterVersion()); 
         System.out.println("== Adapter support overdrive: " + adapter.canOverdrive()); 
         System.out.println("== Adapter support hyperdrive: " + adapter.canHyperdrive()); 
         System.out.println("== Adapter support EPROM programming: " + adapter.canProgram()); 
         System.out.println("== Adapter support power: " + adapter.canDeliverPower()); 
         System.out.println("== Adapter support smart power: " + adapter.canDeliverSmartPower()); 
         System.out.println("== Adapter Class Version: " + adapter.getClassVersion());

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // force first select device
         int main_selection = MAIN_SELECT_DEVICE;

         // loop to do menu
         do
         {
            // Main menu
            switch(main_selection)
            {
               case MAIN_DISPLAY_INFO:
                  // display Switch info
                  printSwitchInfo(sw);
                  break;

               case MAIN_CLEAR_ACTIVITY:
                  sw.clearActivity();
                  state = sw.readDevice();
                  sw.writeDevice(state);
                  // display Switch info
                  printSwitchInfo(sw);
                  break;

               case MAIN_SET_LATCH:
                  state = sw.readDevice();
                  System.out.print("Enter the channel number: ");
                  ch = getNumber(0,sw.getNumberChannels(state) - 1);
                  if (menuSelect(stateMenu) == STATE_ON)
                     sw.setLatchState(ch,true,false,state);                  
                  else
                     sw.setLatchState(ch,false,false,state);                  
                  sw.writeDevice(state);
                  // display Switch info
                  printSwitchInfo(sw);
                  break;

               case MAIN_SELECT_DEVICE:
                  // find all parts
                  owd_vect = findAllSwitchDevices(adapter);
                  // select a device
                  sw = (SwitchContainer)selectDevice(owd_vect);
                  // display device info
                  printDeviceInfo((OneWireContainer)sw);
                  // display the switch info
                  printSwitchInfo(sw);
                  break;               

               case MAIN_QUIT:
                  done = true;
                  break;
            }
            
            if (!done)
               main_selection = menuSelect(mainMenu);

         }
         while (!done);
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

      System.out.println();
      System.exit(0);
   }

   /** 
    * Search for all Switch devices on the provided adapter and return
    * a vector 
    * 
    * @param  adapter valid 1-Wire adapter
    *
    * @return Vector or OneWireContainers
    */
   public static Vector findAllSwitchDevices(DSPortAdapter adapter)
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

            // check if a switch
            if (!(owd instanceof SwitchContainer))
               continue; 

            owd_vect.addElement(owd);

            // set owd to max possible speed with available adapter, allow fall back
            if (adapter.canOverdrive() && (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE))
               owd.setSpeed(owd.getMaxSpeed(),true);
         }
      }
      catch(Exception e)
      {
         System.out.println(e);
      }

      return owd_vect;
   }

   //--------
   //-------- Menu methods
   //--------

   /** 
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    * 
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer selectDevice(Vector owd_vect)
      throws InterruptedException
   {
      // create a menu
      String[] menu = new String[owd_vect.size() + 2];
      OneWireContainer owd;
      int i;

      menu[0] = "Device Selection";
      for (i = 0; i < owd_vect.size(); i++)
      {
         owd = (OneWireContainer)owd_vect.elementAt(i);
         menu[i + 1] = new String("(" + i + ") " + 
                  owd.getAddressAsString() + 
                  " - " + owd.getName());
         if (owd.getAlternateNames().length() > 0)
            menu[i + 1] += "/" + owd.getAlternateNames();
      }         
      menu[i + 1] = new String("[" + i + "]--Quit");

      int select = menuSelect(menu);

      if (select == i)
         throw new InterruptedException("Quit in device selection");

      return (OneWireContainer)owd_vect.elementAt(select);
   }

   //--------
   //-------- Menu Methods
   //--------

   /**
    * Display menu and ask for a selection.
    *
    * @param menu Array of strings that represents the menu.
    *        The first element is a title so skip it.
    *
    * @return numberic value entered from the console.
    */
   static int menuSelect(String[] menu)
   {
      System.out.println();
      for (int i = 0; i < menu.length; i++)
         System.out.println(menu[i]);

      System.out.print("Please enter value: ");

      return getNumber(0, menu.length - 2);
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

            value   = Integer.parseInt(str);

            if ((value < min) || (value > max))
            {
               System.out.println("Invalid value, range must be " + min + " to " + max);
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

   //--------
   //-------- Display Methods
   //--------

   /** 
    * Display information about the 1-Wire device
    * 
    * @param owd OneWireContainer device
    */
   static void printDeviceInfo(OneWireContainer owd)
   {
      System.out.println();
      System.out.println("*************************************************************************");
      System.out.println("* Device Name: " + owd.getName());
      System.out.println("* Device Other Names: " + owd.getAlternateNames());
      System.out.println("* Device Address: " + owd.getAddressAsString());
      System.out.println("* Device Max speed: " + 
          ((owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE) ? "Overdrive" : "Normal"));
      System.out.println("* iButton Description: " + owd.getDescription());
   }

   /** 
    * Display information about the Switch device
    * 
    * @param owd OneWireContainer device
    */
   static void printSwitchInfo(SwitchContainer swd)
      throws OneWireException
   {
      try
      {
         byte[] state = swd.readDevice();

         System.out.println();
         System.out.println("-----------------------------------------------------------------------");
         System.out.println("| Number of channels: " + swd.getNumberChannels(state));
         System.out.println("| Is high-side switch: " + swd.isHighSideSwitch());
         System.out.println("| Has Activity Sensing: " + swd.hasActivitySensing());
         System.out.println("| Has Level Sensing: " + swd.hasLevelSensing());
         System.out.println("| Has Smart-on: " + swd.hasSmartOn());
         System.out.println("| Only 1 channel on at a time: " + swd.onlySingleChannelOn());
         System.out.println();

         System.out.print("    Channel          "); 
         for (int ch = 0; ch < swd.getNumberChannels(state); ch++)
            System.out.print(ch + "      ");
         System.out.println();

         System.out.println("    -----------------------------------");

         System.out.print("    Latch State      "); 
         for (int ch = 0; ch < swd.getNumberChannels(state); ch++)
            System.out.print(((swd.getLatchState(ch,state) == true) ? "ON     " : "OFF    "));
         System.out.println();

         if (swd.hasLevelSensing())
         {
            System.out.print("    Sensed Level     "); 
            for (int ch = 0; ch < swd.getNumberChannels(state); ch++)
               System.out.print(((swd.getLevel(ch,state) == true) ? "HIGH   " : "LOW    "));
            System.out.println();
         }

         if (swd.hasActivitySensing())
         {
            System.out.print("    Sensed Activity  "); 
            for (int ch = 0; ch < swd.getNumberChannels(state); ch++)
               System.out.print(((swd.getSensedActivity(ch,state) == true) ? "SET    " : "CLEAR  "));
            System.out.println();
         }
      }
      catch (OneWireIOException e)
      {
         System.out.println(e);
      }
   }

   //--------
   //-------- Menus
   //--------

   static final String[] mainMenu = { "MainMenu 1-Wire Switch Demo", 
                                      "(0) Dislay switch state",
                                      "(1) Clear activity ",
                                      "(2) Set Latch state", 
                                      "(3) Select new Device",
                                      "(4) Quit" };
   static final int MAIN_DISPLAY_INFO   = 0;
   static final int MAIN_CLEAR_ACTIVITY = 1;
   static final int MAIN_SET_LATCH      = 2;
   static final int MAIN_SELECT_DEVICE  = 3;
   static final int MAIN_QUIT           = 4;

   static final String[] stateMenu = { "Channel State", 
                                      "(0) Off, non-conducting", 
                                      "(1) On, conducting" };
   static final int STATE_OFF          = 0;
   static final int STATE_ON           = 1;
}


