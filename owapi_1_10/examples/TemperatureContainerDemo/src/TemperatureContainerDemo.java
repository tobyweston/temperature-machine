
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


/**
 *   menu driven program to test OneWireContainer with
 *   TemperatureContainer interface
 */
public class TemperatureContainerDemo
{

   // constant for temperature display option
   static final int CELSIUS    = 0x01;
   static final int FAHRENHEIT = 0x02;

   // user main option menu
   static Hashtable hashMainMenu = new Hashtable();
   static int       mainMenuItemCount;

   // temperature display mode
   static int tempMode = CELSIUS;

   // temperature unit
   static String          tempUnit = " C";
   static DSPortAdapter   adapter  = null;
   static BufferedReader  dis      = new BufferedReader(new InputStreamReader(System.in));

   /**
    * Method main
    *
    *
    * @param args
    *
    */
   public static void main (String[] args)
   {
      byte[]               state = null;
      double               alarmLow;
      double               alarmHigh;
      boolean              alarming;
      OneWireContainer     owc = null;
      TemperatureContainer tc  = null;

      // find and initialize the first OneWireContainer with
      // TemperatureContainer interface
      owc = initContainer();

      if (!(owc instanceof TemperatureContainer))
      {
         cleanup();
         System.out.println(
            "*************************************************************************");
         System.out.println("No TemperatureContainer found. Exit program.");
         System.out.println();
         System.exit(0);
      }
      else
         tc = ( TemperatureContainer ) owc;

      initMenu();

      int curMenuChoice = 0;

      while (true)
      {
         curMenuChoice = getMenuChoice(hashMainMenu, mainMenuItemCount);

         try
         {
            switch (curMenuChoice)
            {

               case 0 :   // Read Temperature Once
                  getTemperature(tc, 1);
                  break;
               case 1 :   // Read Temperature Multiple Time
                  System.out.print("Please enter number of times: ");

                  int trial = ( int ) getNumber();

                  getTemperature(tc, trial);
                  break;
               case 2 :   // Read High and Low Alarms
                  if (!tc.hasTemperatureAlarms())
                  {
                     System.out.println("Temperature alarms not supported");
                  }
                  else
                  {
                     state     = tc.readDevice();
                     alarmLow  =
                        tc.getTemperatureAlarm(TemperatureContainer.ALARM_LOW,
                                               state);
                     alarmHigh = tc.getTemperatureAlarm(
                        TemperatureContainer.ALARM_HIGH, state);

                     if (tempMode == FAHRENHEIT)
                     {
                        alarmHigh = convertToFahrenheit(alarmHigh);
                        alarmLow  = convertToFahrenheit(alarmLow);
                     }

                     System.out.println("  Alarm: High = " + alarmHigh
                                        + tempUnit + ", Low = " + alarmLow
                                        + tempUnit);
                  }
                  break;
               case 3 :   // Set  High and Low Alarms
                  System.out.println(
                     "*************************************************************************");

                  if (!tc.hasTemperatureAlarms())
                  {
                     System.out.println("Temperature alarms not supported");
                  }
                  else
                  {
                     if (tempMode == CELSIUS)
                        System.out.println(
                           "*** Temperature value in Celsius ***");
                     else
                        System.out.println(
                           "*** Temperature value in Fehrenheit ***");

                     System.out.print("Enter alarm high value: ");

                     double inputHigh = getNumber();

                     System.out.print("Enter alarm low value: ");

                     double inputLow = getNumber();

                     if (tempMode == CELSIUS)
                     {
                        alarmHigh = inputHigh;
                        alarmLow  = inputLow;
                     }
                     else
                     {
                        alarmHigh = convertToCelsius(inputHigh);
                        alarmLow  = convertToCelsius(inputLow);
                     }

                     state = tc.readDevice();

                     tc.setTemperatureAlarm(TemperatureContainer.ALARM_HIGH,
                                            alarmHigh, state);
                     tc.setTemperatureAlarm(TemperatureContainer.ALARM_LOW,
                                            alarmLow, state);
                     tc.writeDevice(state);

                     state     = tc.readDevice();
                     alarmLow  =
                        tc.getTemperatureAlarm(TemperatureContainer.ALARM_LOW,
                                               state);
                     alarmHigh = tc.getTemperatureAlarm(
                        TemperatureContainer.ALARM_HIGH, state);

                     if (tempMode == FAHRENHEIT)
                     {
                        alarmHigh = convertToFahrenheit(alarmHigh);
                        alarmLow  = convertToFahrenheit(alarmLow);
                     }

                     System.out.println("  Set Alarm: High = " + alarmHigh
                                        + tempUnit + ", Low = " + alarmLow
                                        + tempUnit);
                  }
                  break;
               case 4 :   // isAlarming
                  if (!tc.hasTemperatureAlarms())
                  {
                     System.out.println("Temperature alarms not supported");
                  }
                  else
                  {
                     alarming = owc.isAlarming();

                     if (alarming)
                        System.out.println("  Alarming");
                     else
                        System.out.println("  Not Alarming");
                  }
                  break;
               case 5 :   // Temperature Display Option
                  System.out.println(
                     "*************************************************************************");
                  System.out.println("1. Celsius");
                  System.out.println("2. Fehrenheit");
                  System.out.print(
                     "Please enter temperature display option: ");

                  int choice = ( int ) getNumber();

                  if (choice == 2)
                  {
                     tempMode = FAHRENHEIT;
                     tempUnit = " F";

                     System.out.println("  Set to Fehrenheit display mode ");
                  }
                  else
                  {
                     tempMode = CELSIUS;
                     tempUnit = " C";

                     System.out.println(
                        "  Set to Celsius display mode (default) ");
                  }
                  break;
               case 6 :
                  cleanup();
                  System.exit(0);
                  break;
            }             //switch
         }
         catch (Exception e)
         {
            printException(e);
         }
      }                   // while
   }

   // find the first OneWireContainer with TemperatureContainer
   // interface. If found, initialize the container
   static OneWireContainer initContainer ()
   {
      byte[]               state = null;
      OneWireContainer     owc   = null;
      TemperatureContainer tc    = null;

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // get exclusive use of adapter
         adapter.beginExclusive(true);
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // enumerate through all the one wire device found
         for (Enumeration owc_enum = adapter.getAllDeviceContainers();
                 owc_enum.hasMoreElements(); )
         {

            // get the next owc
            owc = ( OneWireContainer ) owc_enum.nextElement();

            // check for one wire device that implements TemperatureCotainer interface
            if (owc instanceof TemperatureContainer)
            {
               tc = ( TemperatureContainer ) owc;

               // access One Wire device
               state = tc.readDevice();

               if (tc.hasSelectableTemperatureResolution())
               {
                  double[] resolution = tc.getTemperatureResolutions();

                  tc.setTemperatureResolution(
                     resolution [resolution.length - 1], state);
               }

               tc.writeDevice(state);

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

   // read temperature from device
   static void getTemperature (TemperatureContainer tc, int trial)
      throws OneWireException, OneWireIOException
   {
      // get the current resolution and other settings of the device
      byte[] state = tc.readDevice();
      double lastTemp;

      while (trial-- > 0)
      {
         // perform a temperature conversion
         tc.doTemperatureConvert(state);

         // read the result of the conversion
         state = tc.readDevice();

         // extract the result out of state
         lastTemp = tc.getTemperature(state);

         if (tempMode == FAHRENHEIT)
            lastTemp = convertToFahrenheit(lastTemp);

         // show results up to 2 decimal places
         System.out.println("  Temperature = "
                            + (( int ) (lastTemp * 100)) / 100.0 + tempUnit);
      }
   }

   /** initialize menu choices  */
   static void initMenu ()
   {
      hashMainMenu.put(new Integer(0), "Read Temperature Once");
      hashMainMenu.put(new Integer(1), "Read Temperature Multiple Time");
      hashMainMenu.put(new Integer(2), "Read High and Low Alarms");
      hashMainMenu.put(new Integer(3), "Set  High and Low Alarms");
      hashMainMenu.put(new Integer(4), "isAlarming");
      hashMainMenu.put(new Integer(5), "Temperature Display Option");
      hashMainMenu.put(new Integer(6), "Quit");

      mainMenuItemCount = 7;

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

   /** Convert a temperature from Celsius to Fahrenheit. */
   static double convertToFahrenheit (double celsiusTemperature)
   {
      return ( double ) (celsiusTemperature * 9.0 / 5.0 + 32.0);
   }

   /** Convert a temperature from Fahrenheit to Celsius.  */
   static double convertToCelsius (double fahrenheitTemperature)
   {
      return ( double ) ((fahrenheitTemperature - 32.0) * 5.0 / 9.0);
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
