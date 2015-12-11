
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

import java.io.*;
import java.util.*;
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;


/** menu driven program to test OneWireContainer with ADContainer interface */
public class ADContainerDemo
{

   // user main option menu
   static Hashtable       hashMainMenu = new Hashtable();
   static int             mainMenuItemCount;
   static DSPortAdapter   adapter      = null;
   static int             maxNumChan   = 1;      // maximum number of channel
   static boolean         chanSelected = false;   // set to true if user has selected channels
   static String          ADUnit       = " V";   // A/D unit
   static BufferedReader  dis          = new BufferedReader(new InputStreamReader(System.in));

   /**
    * Method main
    *
    *
    * @param args
    *
    */
   public static void main (String[] args)
   {
      OneWireContainer owc = null;
      ADContainer      adc = null;

      // find and initialize the first OneWireContainer with
      // ADContainer interface
      owc = initContainer();

      if (!(owc instanceof ADContainer))
      {
         cleanup();
         System.out.println(
            "*************************************************************************");
         System.out.println("No ADContainer found. Exit program.");
         System.out.println();
         System.exit(0);
      }
      else
         adc = ( ADContainer ) owc;

      maxNumChan = adc.getNumberADChannels();

      // array to determine whether a specific channel has been selected
      boolean[] channel = new boolean [maxNumChan];

      for (int i = 0; i < maxNumChan; i++)   // default, no channel selected
         channel [i] = false;

      byte[]   state     = null;   // data from device
      double[] ranges    = null;   // A/D ranges
      double   alarmLow  = 0;      // alarm low value
      double   alarmHigh = 0;      // alarm high value
      boolean  alarming;

      // temporary storage for user input
      int     inputInt      = 0;
      double  inputDouble   = 0.0;
      double  inputLow      = 0.0;
      double  inputHigh     = 0.0;
      String  inputString   = null;
      boolean moreInput     = true;
      int     curMenuChoice = 0;

      initMenu();

      while (true)
      {
         curMenuChoice = getMenuChoice(hashMainMenu, mainMenuItemCount);

         try
         {
            switch (curMenuChoice)
            {

               case 0 :   // Select Channel
                  System.out.println(
                     "*************************************************************************");
                  System.out.println(
                     "All previously selectd channels have been cleared.");

                  state = adc.readDevice();

                  for (int i = 0; i < maxNumChan; i++)
                  {

                     // clear all channel selection
                     channel [i] = false;

                     if (adc.hasADAlarms())
                     {

                        // disable alarms
                        adc.setADAlarmEnable(i, ADContainer.ALARM_LOW, false,
                                             state);
                        adc.setADAlarmEnable(i, ADContainer.ALARM_HIGH,
                                             false, state);
                     }
                  }

                  adc.writeDevice(state);

                  chanSelected = false;
                  state        = adc.readDevice();

                  int count = 1;

                  moreInput = true;

                  while (moreInput && (count <= maxNumChan))
                  {
                     System.out.print("Please enter channel # " + count
                                      + " ( Enter -1 if no more): ");

                     inputInt = ( int ) getNumber();

                     if (inputInt == -1)
                     {
                        moreInput = false;
                     }
                     else
                     {
                        if (isChannelValid(inputInt))
                        {
                           channel [inputInt] = true;

                           count++;

                           chanSelected = true;

                           if (adc.hasADAlarms())
                           {

                              // enable alarms
                              adc.setADAlarmEnable(inputInt,
                                                   ADContainer.ALARM_LOW,
                                                   true, state);
                              adc.setADAlarmEnable(inputInt,
                                                   ADContainer.ALARM_HIGH,
                                                   true, state);
                           }
                        }
                     }
                  }       // while (moreInput && (count <= maxNumChan))

                  adc.writeDevice(state);
                  System.out.print("  Channels to monitor = ");

                  if (count == 1)
                     System.out.println("NONE");
                  else
                  {
                     for (int i = 0; i < maxNumChan; i++)
                        if (channel [i])
                           System.out.print(i + " ");

                     System.out.println();
                  }
                  break;
               case 1 :   // Get A/D Once
                  getVoltage(adc, channel, 1);
                  break;
               case 2 :   // Get A/D Multiple Time
                  System.out.print("Please enter number of times: ");

                  inputInt = ( int ) getNumber();

                  getVoltage(adc, channel, inputInt);
                  break;
               case 3 :   // Get A/D ranges
                  if (!chanSelected)
                  {
                     System.out.println(
                        "No channel selected yet. Cannot get A/D ranges.");
                  }
                  else
                  {
                     state = adc.readDevice();

                     for (int i = 0; i < maxNumChan; i++)
                     {
                        if (channel [i])
                        {
                           ranges = adc.getADRanges(i);

                           System.out.print("Ch " + i + " - Available: "
                                            + ranges [0] + ADUnit);

                           for (int j = 1; j < ranges.length; j++)
                              System.out.print(", " + ranges [j] + ADUnit);

                           System.out.println(". Current: "
                                              + adc.getADRange(i, state)
                                              + ADUnit);
                        }
                     }
                  }
                  break;
               case 4 :   // Set A/D ranges
                  System.out.println(
                     "*************************************************************************");

                  state     = adc.readDevice();
                  moreInput = true;

                  while (moreInput)
                  {
                     System.out.print("Please enter channel number: ");

                     inputInt = ( int ) getNumber();

                     if (isChannelValid(inputInt))
                     {
                        System.out.print("Please enter range value: ");

                        inputDouble = getNumber();

                        adc.setADRange(inputInt, inputDouble, state);
                        adc.writeDevice(state);

                        state = adc.readDevice();

                        System.out.println("  Ch" + inputInt
                                           + " A/D Ranges set to: "
                                           + adc.getADRange(inputInt, state));
                        System.out.print("Set more A/D ranges (Y/N)? ");

                        inputString = dis.readLine();

                        if (!inputString.trim().toUpperCase().equals("Y"))
                           moreInput = false;
                     }
                  }   // while(moreInput)
                  break;
               case 5 :   // Get High and Low Alarms
                  if (!adc.hasADAlarms())
                  {
                     System.out.println("A/D alarms not supported");
                  }
                  else if (!chanSelected)
                  {
                     System.out.println(
                        "No channel selected yet. Cannot get high and low alarms.");
                  }
                  else
                  {
                     state = adc.readDevice();

                     for (int i = 0; i < maxNumChan; i++)
                     {
                        if (channel [i])
                        {
                           alarmLow  =
                              adc.getADAlarm(i, ADContainer.ALARM_LOW, state);
                           alarmHigh =
                              adc.getADAlarm(i, ADContainer.ALARM_HIGH,
                                             state);

                           // show results up to 2 decimal places
                           System.out.println(
                              "Ch " + i + " Alarm: High = "
                              + (( int ) (alarmHigh * 100)) / 100.0 + ADUnit
                              + ", Low = "
                              + (( int ) (alarmLow * 100)) / 100.0 + ADUnit);
                        }
                     }   // for
                  }
                  break;
               case 6 :   // Set High and Low Alarms
                  if (!adc.hasADAlarms())
                  {
                     System.out.println("A/D alarms not supported");
                  }
                  else
                  {
                     System.out.println(
                        "*************************************************************************");

                     state     = adc.readDevice();
                     moreInput = true;

                     while (moreInput)
                     {
                        System.out.print("Please enter channel number: ");

                        inputInt = ( int ) getNumber();

                        if (isChannelValid(inputInt))
                        {
                           boolean inputValid = false;

                           while (!inputValid)
                           {
                              System.out.print(
                                 "Please enter alarm high value: ");

                              inputHigh = getNumber();

                              if (inputHigh > adc.getADRange(inputInt, state))
                                 System.out.println(
                                    "Current A/D range is: "
                                    + adc.getADRange(inputInt, state)
                                    + ADUnit + ". Invalid alarm high value.");
                              else
                                 inputValid = true;
                           }

                           System.out.print("Please enter alarm low value: ");

                           inputLow = getNumber();

                           adc.setADAlarm(inputInt, ADContainer.ALARM_LOW,
                                          inputLow, state);
                           adc.setADAlarm(inputInt, ADContainer.ALARM_HIGH,
                                          inputHigh, state);
                           adc.writeDevice(state);

                           state = adc.readDevice();

                           // show results up to 2 decimal places
                           System.out.println(
                              "  Set Ch" + inputInt + " Alarm: High = "
                              + (( int ) (adc.getADAlarm(
                              inputInt, ADContainer.ALARM_HIGH,
                                 state) * 100)) / 100.0 + ADUnit + ", Low = "
                                                        + (( int ) (adc.getADAlarm(
                                                        inputInt,
                                                        ADContainer.ALARM_LOW,
                                                        state) * 100)) / 100.0 + ADUnit);
                           System.out.print("Set more A/D alarms (Y/N)? ");

                           inputString = dis.readLine();

                           if (!inputString.trim().toUpperCase().equals("Y"))
                              moreInput = false;
                        }
                     }   // while(moreInput)
                  }
                  break;
               case 7 :   // hasADAlarmed
                  if (!adc.hasADAlarms())
                  {
                     System.out.println("A/D alarms not supported");
                  }
                  else
                  {
                     alarming = owc.isAlarming();

                     if (alarming)
                     {
                        System.out.print("  Alarms: ");

                        state = adc.readDevice();

                        for (int i = 0; i < maxNumChan; i++)
                        {
                           if (channel [i])
                           {
                              if (adc.hasADAlarmed(i, ADContainer.ALARM_HIGH,
                                                   state))
                                 System.out.print("Ch" + i
                                                  + " alarmed high; ");

                              if (adc.hasADAlarmed(i, ADContainer.ALARM_LOW,
                                                   state))
                                 System.out.print("Ch" + i
                                                  + " alarmed low; ");
                           }
                        }

                        System.out.println();
                     }
                     else
                        System.out.println("  Not Alarming");
                  }
                  break;
               case 8 :
                  cleanup();
                  System.exit(0);
                  break;
            }
         }
         catch (Exception e)
         {
            printException(e);
         }
      }   // while
   }

   // find the first OneWireContainer with ADContainer interface
   // if found, initialize the container
   static OneWireContainer initContainer ()
   {
      byte[]           state = null;
      OneWireContainer owc   = null;
      ADContainer      adc   = null;

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // get exclusive use of adapter
         adapter.beginExclusive(true);
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // enumerate through all the One Wire device found
         for (Enumeration owc_enum = adapter.getAllDeviceContainers();
                 owc_enum.hasMoreElements(); )
         {

            // get the next owc
            owc = ( OneWireContainer ) owc_enum.nextElement();

            // check for One Wire device that implements ADCotainer interface
            if (owc instanceof ADContainer)
            {
               adc = ( ADContainer ) owc;

               // access One Wire device
               state = adc.readDevice();

               double[] range      = null;
               double[] resolution = null;

               // set resolution
               for (int channel = 0; channel < adc.getNumberADChannels();
                       channel++)
               {
                  range      = adc.getADRanges(channel);
                  resolution = adc.getADResolutions(channel, range [0]);

                  // set to largest range
                  adc.setADRange(channel, range [0], state);

                  // set to highest resolution
                  adc.setADResolution(channel,
                                      resolution [resolution.length - 1],
                                      state);

                  if (adc.hasADAlarms())
                  {

                     // disable all alarms
                     adc.setADAlarmEnable(channel, ADContainer.ALARM_LOW,
                                          false, state);
                     adc.setADAlarmEnable(channel, ADContainer.ALARM_HIGH,
                                          false, state);
                  }
               }

               adc.writeDevice(state);

               // print device information
               System.out.println();
               System.out.println(
                  "*************************************************************************");
               System.out.println("* 1-Wire Device Name: " + owc.getName());
               System.out.println("* 1-Wire Device Other Names: "
                                  + owc.getAlternateNames());
               System.out.println("* 1-Wire Device Address: "
                                  + owc.getAddressAsString());
               System.out.println(
                  "* 1-Wire Device Max speed: "
                  + ((owc.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE)
                     ? "Overdrive"
                     : "Normal"));
               System.out.println("* 1-Wire Device Number of Channels: "
                                  + adc.getNumberADChannels());
               System.out.println("* 1-Wire Device Can Read MultiChannels: "
                                  + adc.canADMultiChannelRead());
               System.out.println("* 1-Wire Device Description: "
                                  + owc.getDescription());
               System.out.println(
                  "*************************************************************************");
               System.out.println("  Hit ENTER to continue...");
               dis.readLine();

               break;
            }
         }   // enum all owc
      }
      catch (Exception e)
      {
         printException(e);
      }

      return owc;
   }

   // read A/D from device
   static void getVoltage (ADContainer adc, boolean[] channel, int trial)
      throws OneWireException, OneWireIOException
   {
      byte[]   state;
      double[] curVoltage = new double [channel.length];

      if (!chanSelected)
      {
         System.out.println(
            "No channel selected yet. Cannot get voltage reading.");

         return;
      }

      while (trial-- > 0)
      {
         state = adc.readDevice();

         if (adc.canADMultiChannelRead())
         {

            // do all channels together
            adc.doADConvert(channel, state);

            curVoltage = adc.getADVoltage(state);
         }
         else
         {

            // do one channel at a time;
            for (int i = 0; i < maxNumChan; i++)
            {
               if (channel [i])
               {
                  adc.doADConvert(i, state);

                  curVoltage [i] = adc.getADVoltage(i, state);
               }
            }
         }

         System.out.print("  Voltage Reading:");

         for (int i = 0; i < maxNumChan; i++)
         {
            if (channel [i])   // show value up to 2 decimal places
               System.out.print(" Ch" + i + " = "
                                + (( int ) (curVoltage [i] * 10000)) / 10000.0
                                + ADUnit);
         }

         System.out.println();
      }
   }

   /** initialize menu choices  */
   static void initMenu ()
   {
      hashMainMenu.put(new Integer(0), "Select Channel");
      hashMainMenu.put(new Integer(1), "Get Voltage Once");
      hashMainMenu.put(new Integer(2), "Get Voltage Multiple Time");
      hashMainMenu.put(new Integer(3), "Get A/D Ranges");
      hashMainMenu.put(new Integer(4), "Set A/D Ranges");
      hashMainMenu.put(new Integer(5), "Get High and Low A/D Alarms");
      hashMainMenu.put(new Integer(6), "Set High and Low A/D Alarms");
      hashMainMenu.put(new Integer(7), "hasADAlarmed");
      hashMainMenu.put(new Integer(8), "Quit");

      mainMenuItemCount = 9;

      return;
   }

   /** getMenuChoice - retrieve menu choice from the user  */
   static int getMenuChoice (Hashtable menu, int count)
   {
      int     choice      = 0;

      while (true)
      {
         System.out.println(
            "*************************************************************************");

         for (int i = 0; i < count; i++)
            System.out.println(i + ". " + menu.get(new Integer(i)));

         System.out.print("Please enter your choice: ");

         // change input into integer number
         choice = ( int ) getNumber();

         if (menu.get(new Integer(choice)) == null)
         {
             System.out.println("Invalid menu choice");
         }
         else
             break;
      }

      return choice;
   }

   /** check for valid channel number input */
   static boolean isChannelValid (int channel)
   {
      if ((channel < 0) || (channel >= maxNumChan))
      {
         System.out.println("Channel number has to be between 0 and "
                            + (maxNumChan - 1));

         return false;
      }
      else
         return true;
   }

   /**
    * Retrieve user input from the console.
    *
    * @return numberic value entered from the console.
    *
    */
   static double getNumber ()
   {
      double  value   = -1;
      String  input;

      while (true)
      {
         try
         {
            input   = dis.readLine();
            value   = Double.valueOf(input).doubleValue();
            break;
         }
         catch (NumberFormatException e)
         {
            System.out.println("Invalid Numeric Value: " + e.toString());
            System.out.print("Please enter value again: ");
         }
         catch (java.io.IOException e)
         {
            System.out.println("Error in reading from console: " + e);
         }
         catch (Exception e)
         {
            printException(e);
         }
      }

      return value;
   }

   /** print out Exception stack trace */
   static void printException (Exception e)
   {
      System.out.println("***** EXCEPTION *****");
      e.printStackTrace();
   }

   /** clean up before exiting program */
   static void cleanup ()
   {
      try
      {
         if (adapter != null)
         {
            adapter.endExclusive(); // end exclusive use of adapter
            adapter.freePort();     // free port used by adapter
         }
      }
      catch (Exception e)
      {
         printException(e);
      }

      return;
   }
}
