
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


public class dumpMission
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
         "DS1921 Thermocron Java Demo: Mission Reading Program.\r\n");
      System.out.println("Usage: ");
      System.out.println("   java dumpMission ADAPTER_PORT OPTIONS\r\n");
      System.out.println(
         "ADAPTER_PORT is a String that contains the name of the");
      System.out.println(
         "adapter you would like to use and the port you would like");
      System.out.println("to use, for example: ");
      System.out.println("   java dumpMission {DS1410E}_LPT1\r\n");
      System.out.println(
         "OPTIONS is a String that includes zero or more of the following:");
      System.out.println(
         "   a  Print the alarm violation history for the mission.");
      System.out.println("   h  Print the histogram for the mission.");
      System.out.println(
         "   l  Print the log of temperatures for the mission.");
      System.out.println(
         "   k  Kill the current mission before reading stats.");
      System.out.println(
         "   s  Stop the current mission after reading stats.");
      System.out.println("Example: ");
      System.out.println("   java dumpMission {DS1410E}_LPT1 ahl\r\n");
      System.out.println(
         "You can leave ADAPTER_PORT blank to use the default one-wire adapter and port.");
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
      boolean       show_history   = false;
      boolean       show_log       = false;
      boolean       show_histogram = false;
      boolean       pre_kill       = false;
      boolean       post_kill      = false;
      boolean       usedefault     = false;
      DSPortAdapter access         = null;
      String        adapter_name   = null;
      String        port_name      = null;

      if ((args == null) || (args.length < 1)
              || (args [0].indexOf("_") == -1))
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

      if ((args != null) && (args.length > 0) && (usedefault))
      {
         String arg = args [0];

         if (arg.indexOf("a") != -1)
            show_history = true;

         if (arg.indexOf("l") != -1)
            show_log = true;

         if (arg.indexOf("h") != -1)
            show_histogram = true;

         if (arg.indexOf("k") != -1)
            pre_kill = true;

         if (arg.indexOf("s") != -1)
            post_kill = true;
      }

      if (!usedefault)
      {
         StringTokenizer st = new StringTokenizer(args [0], "_");

         if (st.countTokens() != 2)
         {
            printUsageString();

            return;
         }

         if (args.length > 1)
         {
            String arg = args [1];

            if (arg.indexOf("a") != -1)
               show_history = true;

            if (arg.indexOf("l") != -1)
               show_log = true;

            if (arg.indexOf("h") != -1)
               show_histogram = true;

            if (arg.indexOf("k") != -1)
               pre_kill = true;

            if (arg.indexOf("s") != -1)
               post_kill = true;
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
         System.out.println("Could not find any DS1921 Thermocrons!");

         return;
      }

      OneWireContainer21 owc = new OneWireContainer21();

      owc.setupContainer(access, access.getAddressAsLong());

      //put the part into overdrive...make it sizzle!
      owc.setSpeed(access.SPEED_OVERDRIVE, true);

      //let's gather our information here...
      long t1 = System.currentTimeMillis();

      if (pre_kill)
      {
         try
         {
            owc.disableMission();
         }
         catch (Exception e)
         {
            System.out.println("Couldn't end mission before reading: "
                               + e.toString());
         }
      }

      boolean  mission_in_progress =
         owc.getFlag(owc.STATUS_REGISTER, owc.MISSION_IN_PROGRESS_FLAG);
      byte[]   state;

      Calendar cal = Calendar.getInstance();

      // first, check to make sure that the thermochron isn't
      // sampling, or at least that a sample isn't about to occur.
      boolean isSampling = false;
      do
      {
         state = owc.readDevice();
         cal.setTime(new Date(owc.getClock(state)));

         isSampling = mission_in_progress && (
            owc.getFlag(owc.STATUS_REGISTER,
                     owc.SAMPLE_IN_PROGRESS_FLAG, state) ||
            (cal.get(Calendar.SECOND)>55) );

         if(isSampling)
         {
            // wait for current sample to finish
            try
            {
               Thread.sleep(1000);
            }
            catch(InterruptedException ie)
            {;}
         }
      }
      while(isSampling);

      int      mission_count       = owc.getMissionSamplesCounter(state);
      int      device_count        = owc.getDeviceSamplesCounter(state);
      long     rtc                 = owc.getClock(state);
      long     next_alarm          = owc.getClockAlarm(state);
      Calendar time_stamp          = owc.getMissionTimeStamp(state);
      int      sample_rate         = owc.getSampleRate(state);
      double   high_alarm          = owc.getTemperatureAlarm(owc.ALARM_HIGH,
                                        state);
      double   low_alarm           = owc.getTemperatureAlarm(owc.ALARM_LOW,
                                        state);
      int[]    histogram           = owc.getTemperatureHistogram();
      byte[]   log                 = owc.getTemperatureLog(state);
      byte[]   high_history        =
         owc.getAlarmHistory(owc.TEMPERATURE_HIGH_ALARM);
      byte[]   low_history         =
         owc.getAlarmHistory(owc.TEMPERATURE_LOW_ALARM);
      long     t2                  = System.currentTimeMillis();
      boolean  clock_enabled       = owc.isClockRunning(state);
      boolean  alarm_enabled       = owc.isClockAlarmEnabled(state);
      boolean  clock_alarm         = owc.isClockAlarming(state);
      boolean  rollover            = owc.getFlag(owc.CONTROL_REGISTER,
                                                 owc.ROLLOVER_ENABLE_FLAG,
                                                 state);
      double   current_temp        = 0;
      String   mission_in_progress_string;

      if (!mission_in_progress)
      {
         owc.doTemperatureConvert(state);

         current_temp               = owc.getTemperature(state);
         mission_in_progress_string = "- NO MISSION IN PROGRESS AT THIS TIME";
      }
      else
         mission_in_progress_string = "- MISSION IS CURRENTLY RUNNING";

      //spew all this data
      BufferedReader in =
         new BufferedReader(new InputStreamReader(System.in));

      System.out.println(
         "Dallas Semiconductor DS1921 Thermocron Mission Summary Demo");
      System.out.println(
         "-----------------------------------------------------------");
      System.out.println("- Device ID : " + owc.getAddressAsString());
      System.out.println(mission_in_progress_string);

      if (!mission_in_progress)
      {
         System.out.println("- Current Temperature : " + current_temp);
      }
      else
         System.out.println(
            "- Cannot read current temperature with mission in progress");

      System.out.println(
         "-----------------------------------------------------------");
      System.out.println("- Number of mission samples: " + mission_count);
      System.out.println("- Total number of samples  : " + device_count);
      System.out.println("- Real Time Clock          : "
                         + (clock_enabled ? "ENABLED"
                                          : "DISABLED"));
      System.out.println("- Real Time Clock Value    : " + (new Date(rtc)));
      System.out.println("- Clock Alarm              : "
                         + (alarm_enabled ? "ENABLED"
                                          : "DISABLED"));

      if (alarm_enabled)
      {
         System.out.println("- Clock Alarm Status       : "
                            + (clock_alarm ? "ALARMING"
                                           : "NOT ALARMING"));
         System.out.println("- Next alarm occurs at     : "
                            + (new Date(next_alarm)));
      }

      System.out.println("- Last mission started     : "
                         + time_stamp.getTime());
      System.out.println("- Sample rate              : Every " + sample_rate
                         + " minutes");
      System.out.println("- High temperature alarm   : " + high_alarm);
      System.out.println("- Low temperature alarm    : " + low_alarm);
      System.out.println("- Rollover enabled         : " + (rollover ? "YES"
                                                                     : "NO"));
      System.out.println("- Time to read Thermocron  : " + (t2 - t1)
                         + " milliseconds");
      System.out.println(
         "-----------------------------------------------------------");

      if (show_history)
      {
         int start_offset, violation_count;

         System.out.println("-");
         System.out.println("-                   ALARM HISTORY");

         if (low_history.length == 0)
         {
            System.out.println(
               "- No violations against the low temperature alarm.");
            System.out.println("-");
         }

         for (int i = 0; i < low_history.length / 4; i++)
         {
            start_offset    = (low_history [i * 4] & 0x0ff)
                              | ((low_history [i * 4 + 1] << 8) & 0x0ff00)
                              | ((low_history [i * 4 + 2] << 16) & 0x0ff0000);
            violation_count = 0x0ff & low_history [i * 4 + 3];

            System.out.println("- Low alarm started at     : "
                               + (start_offset * sample_rate));
            System.out.println("-                          : Lasted "
                               + (violation_count * sample_rate)
                               + " minutes");
         }

         if (high_history.length == 0)
         {
            System.out.println(
               "- No violations against the high temperature alarm.");
            System.out.println("-");
         }

         for (int i = 0; i < high_history.length / 4; i++)
         {
            start_offset    = (high_history [i * 4] & 0x0ff)
                              | ((high_history [i * 4 + 1] << 8) & 0x0ff00)
                              | ((high_history [i * 4 + 2] << 16)
                                 & 0x0ff0000);
            violation_count = 0x0ff & high_history [i * 4 + 3];

            System.out.println("- High alarm started at    : "
                               + (start_offset * sample_rate));
            System.out.println("-                          : Lasted "
                               + (violation_count * sample_rate)
                               + " minutes");
         }

         System.out.println(
            "-----------------------------------------------------------");
      }

      if (show_log)
      {
         long time = time_stamp.getTime().getTime()
                     + owc.getFirstLogOffset(state);

         System.out.println("-");
         System.out.println("-                   TEMPERATURE LOG");

         GregorianCalendar gc = new GregorianCalendar();
         for (int i = 0; i < log.length; i++)
         {
            gc.setTime(new Date(time));
            System.out.println("- Temperature recorded at  : "
                               + (gc.getTime()));
            System.out.println("-                     was  : "
                               + owc.decodeTemperature(log [i]) + " C");

            time += sample_rate * 60 * 1000;
         }

         System.out.println(
            "-----------------------------------------------------------");
      }

      if (show_histogram)
      {
         double resolution = owc.getTemperatureResolution();
         double histBinWidth = owc.getHistogramBinWidth();
         double start = owc.getHistogramLowTemperature();

         System.out.println("-");
         System.out.println("-                   TEMPERATURE HISTOGRAM");

         for (int i = 0; i < histogram.length; i++)
         {
            System.out.println("- Histogram entry          : "
                               + histogram [i] + " at temperature "
                               + start + " to "
                               + (start + (histBinWidth - resolution)) + " C");

            start += histBinWidth;
         }
      }

      if (post_kill)
      {
         try
         {
            owc.disableMission();
         }
         catch (Exception e)
         {
            System.out.println("Couldn't end mission after reading: "
                               + e.toString());
         }
      }

      access.endExclusive();
      access.freePort();
      access = null;
      System.out.println("Finished.");
   }
}
