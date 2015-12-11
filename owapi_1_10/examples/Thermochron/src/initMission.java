
/*---------------------------------------------------------------------------
 * Copyright (C) 1999-2001 Dallas Semiconductor Corporation, All Rights Reserved.
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

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import java.util.Vector;
import java.io.*;
import com.dalsemi.onewire.utils.CRC16;
import java.util.*;


public class initMission
{
   static int parseInt (BufferedReader in, int def)
   {
      try
      {
         return Integer.parseInt(in.readLine());
      }
      catch (Exception e)
      {
         return def;
      }
   }

   /**
    * Method printUsageString
    *
    *
    */
   public static void printUsageString ()
   {
      System.out.println(
         "DS1921 Thermochron Mission Initialization Program.\r\n");
      System.out.println("Usage: ");
      System.out.println("   java initcopr ADAPTER_PORT\r\n");
      System.out.println(
         "ADAPTER_PORT is a String that contains the name of the");
      System.out.println(
         "adapter you would like to use and the port you would like");
      System.out.println("to use, for example: ");
      System.out.println("   java initcopr {DS1410E}_LPT1");
      System.out.println(
         "You can leave ADAPTER_PORT blank to use the default one-wire adapter and port.");
   }

   /**
    * Method main
    *
    *
    * @param args
    *
    * @throws IOException
    * @throws OneWireException
    * @throws OneWireIOException
    *
    */
   public static void main (String[] args)
      throws OneWireIOException, OneWireException, IOException
   {
      boolean       usedefault   = false;
      DSPortAdapter access       = null;
      String        adapter_name = null;
      String        port_name    = null;

      if ((args == null) || (args.length < 1))
      {
         try
         {
            access = OneWireAccessProvider.getDefaultAdapter();

            if (access == null)
               throw new Exception();
         }
         catch (Exception e)
         {
            System.out.println("Couldn't get default adapter!");
            printUsageString();

            return;
         }

         usedefault = true;
      }

      if (!usedefault)
      {
         StringTokenizer st = new StringTokenizer(args [0], "_");

         if (st.countTokens() != 2)
         {
            printUsageString();

            return;
         }

         adapter_name = st.nextToken();
         port_name    = st.nextToken();

         System.out.println("Adapter Name: " + adapter_name);
         System.out.println("Port Name: " + port_name);
      }

      if (access == null)
      {
         try
         {
            access = OneWireAccessProvider.getAdapter(adapter_name,
                                                      port_name);
         }
         catch (Exception e)
         {
            System.out.println(
               "That is not a valid adapter/port combination.");

            Enumeration en = OneWireAccessProvider.enumerateAllAdapters();

            while (en.hasMoreElements())
            {
               DSPortAdapter temp = ( DSPortAdapter ) en.nextElement();

               System.out.println("Adapter: " + temp.getAdapterName());

               Enumeration f = temp.getPortNames();

               while (f.hasMoreElements())
               {
                  System.out.println("   Port name : "
                                     + (( String ) f.nextElement()));
               }
            }

            return;
         }
      }

      access.adapterDetected();
      access.targetFamily(0x21);
      access.beginExclusive(true);
      access.reset();
      access.setSearchAllDevices();

      boolean next = access.findFirstDevice();

      if (!next)
      {
         System.out.println("Could not find any DS1921 Thermochrons!");

         return;
      }

      OneWireContainer21 owc = new OneWireContainer21();

      owc.setupContainer(access, access.getAddressAsLong());

      BufferedReader in =
         new BufferedReader(new InputStreamReader(System.in));

      //the following section of code jus gets all these options from the command line
      //to see how to actually talk to the iButton, scroll down until you find
      //the code "disableMission()
      System.out.println("Dallas Semiconductor DS1921 Thermochron Demo");
      System.out.println("--------------------------------------------");
      System.out.println("Initializing mission on iButton "
                         + owc.getAddressAsString() + "\r\n");

      String  temp;
      boolean rollover = false;

      System.out.print("Enable rollover (y or n)? ");

      temp = in.readLine();

      if (temp.equalsIgnoreCase("y") || temp.equalsIgnoreCase("yes"))
         rollover = true;

      System.out.print("Enter low temperature alarm in celsius (23) : ");

      int low = parseInt(in, 23);

      System.out.print("Enter high temperature alarm in celsius (28) : ");

      int     high       = parseInt(in, 28);
      boolean clockalarm = false;

      System.out.print("Enable clock alarm (y or n)? ");

      temp = in.readLine();

      if (temp.equalsIgnoreCase("y") || temp.equalsIgnoreCase("yes"))
         clockalarm = true;

      int second    = 0;
      int minute    = 0;
      int hour      = 0;
      int day       = 0;
      int frequency = -1;

      if (clockalarm)
      {
         System.out.println("Clock alarm enabled.  Enter alarm frequency: ");
         System.out.println("   0  Once per second");
         System.out.println("   1  Once per minute");   //need second
         System.out.println("   2  Once per hour");     //need second, minute
         System.out.println("   3  Once per day");   //need hour, minute, second
         System.out.println("   4  Once per week");   //need hour, minute, second, day
         System.out.print("   ? ");

         int x = parseInt(in, -1);

         if ((x < 0) || (x > 4))
         {
            System.out.println("That is not a valid clock alarm frequency.");

            return;
         }

         switch (x)                                     //noteice no breaks!
         {

            case 4 :
               if (frequency == -1)
                  frequency = owc.ONCE_PER_WEEK;

               System.out.print("Day of week to alarm (1==Sunday) : ");

               day = parseInt(in, 1);
            case 3 :
               if (frequency == -1)
                  frequency = owc.ONCE_PER_DAY;

               System.out.print("Hour of day to alarm (0 - 23) : ");

               hour = parseInt(in, 0);
            case 2 :
               if (frequency == -1)
                  frequency = owc.ONCE_PER_HOUR;

               System.out.print("Minute of hour to alarm (0 - 59) : ");

               minute = parseInt(in, 0);
            case 1 :
               if (frequency == -1)
                  frequency = owc.ONCE_PER_MINUTE;

               System.out.print("Second of minute to alarm (0 - 59) : ");

               second = parseInt(in, 0);
            case 0 :
               if (frequency == -1)
                  frequency = owc.ONCE_PER_SECOND;
         }
      }

      boolean synchronize = false;

      System.out.print("Set thermochron clock to system clock (y or n)? ");

      temp = in.readLine();

      if (temp.equalsIgnoreCase("y") || temp.equalsIgnoreCase("yes"))
         synchronize = true;

      System.out.print("Start the mission in how many minutes? ");

      int delay = 0;

      delay = parseInt(in, 0);

      System.out.print("Sampling Interval in minutes (1 to 255)? ");

      int rate = 1;

      rate = parseInt(in, 1);

      //now do some bounds checking
      if (rate < 1)
         rate = 1;

      if (rate > 255)
         rate = 255;

      delay = delay & 0x0ffff;

      int physicalLow = (int) owc.getPhysicalRangeLowTemperature();
      int physicalHigh = (int) owc.getPhysicalRangeHighTemperature();
      if (low < physicalLow)
         low = physicalLow;

      if (low > physicalHigh)
         low = physicalHigh;

      if (high < physicalLow)
         high = physicalLow;

      if (high > physicalHigh)
         high = physicalHigh;

      if (day < 1)
         day = 1;

      if (day > 7)
         day = 7;

      second = second % 60;
      minute = minute % 60;
      hour   = hour % 24;

      //some regurgitation first....
      System.out.println("\r\n\r\nSummary---------------------");
      System.out.println("Rollover enabled              : " + rollover);
      System.out.println("Low temperature alarm trigger : " + low);
      System.out.println("High temperature alarm trigger: " + high);
      System.out.println("Clock alarm enabled           : " + clockalarm);

      if (clockalarm)
      {
         System.out.print("Alarm frequency               : ");

         switch (frequency)
         {

            case OneWireContainer21.ONCE_PER_SECOND :
               System.out.println("Once per second");
               break;
            case OneWireContainer21.ONCE_PER_MINUTE :
               System.out.println("Once per minute");
               break;
            case OneWireContainer21.ONCE_PER_HOUR :
               System.out.println("Once per hour");
               break;
            case OneWireContainer21.ONCE_PER_DAY :
               System.out.println("Once per day");
               break;
            case OneWireContainer21.ONCE_PER_WEEK :
               System.out.println("Once per week");
               break;
            default :
               System.out.println("Unknown alarm frequency!!! Bailing!!!");

               return;
         }

         System.out.print("Alarm setting                 : " + hour + ":"
                          + minute + ":" + second + " ");

         switch (day)
         {

            case 1 :
               System.out.println("Sunday");
               break;
            case 2 :
               System.out.println("Monday");
               break;
            case 3 :
               System.out.println("Tuesday");
               break;
            case 4 :
               System.out.println("Wednesday");
               break;
            case 5 :
               System.out.println("Thursday");
               break;
            case 6 :
               System.out.println("Friday");
               break;
            case 7 :
               System.out.println("Saturday");
               break;
            default :
               System.out.println("Unknown day of week! Bailing!");

               return;
         }
      }

      System.out.println("Synchonizing with host clock  : " + synchronize);
      System.out.println("Mission starts in (minutes)   : " + delay);
      System.out.println("Sampling rate (minutes)       : " + rate);
      System.out.println("-------------------------------\r\n");

      //now let's start talking to the thermochron
      //first lets put it into overdrive
      System.out.println("Putting the part into overdrive...");
      owc.setSpeed(access.SPEED_OVERDRIVE, true);
      System.out.println("Disabling current mission...");
      owc.disableMission();
      System.out.println("Clearing memory...");
      owc.clearMemory();
      System.out.println("Reading device state...");

      byte[] state = owc.readDevice();

      System.out.println("Setting rollover flag in state...");
      owc.setFlag(owc.CONTROL_REGISTER, owc.ROLLOVER_ENABLE_FLAG, rollover,
                  state);
      System.out.println("Setting high alarm in state...");
      owc.setTemperatureAlarm(owc.ALARM_HIGH, ( double ) high, state);
      System.out.println("Setting low alarm in state...");
      owc.setTemperatureAlarm(owc.ALARM_LOW, ( double ) low, state);

      if (clockalarm)
      {
         System.out.println("Setting clock alarm in state...");
         owc.setClockAlarm(hour, minute, second, day, frequency, state);
         System.out.println("Enabling clock alarm in state...");
         owc.setClockAlarmEnable(true, state);
      }

      if (synchronize)
      {
         System.out.println("Synchonizing with host clock in state...");
         owc.setClock(System.currentTimeMillis(), state);
      }

      System.out.println("Setting mission delay in state...");
      owc.setMissionStartDelay(delay, state);
      System.out.println("Enabling the clock oscillator in state...");
      owc.setClockRunEnable(true, state);
      System.out.println("Writing state back to Thermochron...");
      owc.writeDevice(state);
      System.out.println("Enabling mission...");
      owc.enableMission(rate);
      System.out.println("Initialization Complete.");

      //       state = owc.readDevice();
      //       for (int i=0;i<state.length;i++)
      //           System.out.println("State["+(i < 0x10 ? "0" : "")+Integer.toHexString(i)+"] == "+Integer.toHexString(0x0ff & state[i]));
   }
}
