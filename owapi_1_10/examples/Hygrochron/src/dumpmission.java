/*---------------------------------------------------------------------------
 * Copyright (C) 2001 Dallas Semiconductor Corporation, All Rights Reserved.
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
import com.dalsemi.onewire.utils.*;
import java.util.*;

/**
 * Dumps the mission from a DS1922/DS2422 temperature and A-D/Humidity
 * data-logger.  If this is a DS1922H, all data values will be converted
 * and displayed as humidity values.
 *
 * @version 1.00, 14 Aug, 2003
 * @author  shughes
 */
public class dumpmission
{
   /** usage string, all available command-line switches */
   static String[][] usageString = new String[][]
      {
         {"-hideTemp", "", "if present, temperature values will be suppressed in output"},
         {"-hideData", "", "if present, data values will be suppressed in output"},
         {"-stopMission", "", "if present, mission will be stopped before the data is retrieved"},
         {"-useOverdrive", "", "if present, mission data will be collected in overdrive speed"},
         {"-readWritePass", "H", "This is the read/write password to use for reading the mission, H=8 bytes of ascii-encoded hex"},
         {"-readOnlyPass", "H", "This is the read-only password to use for reading the mission, H=8 bytes of ascii-encoded hex"}
      };

   /** prints out a usage string and exits */
   public static void usage()
   {
      System.out.println();
      System.out.println("switches:");
      for(int i=0; i<usageString.length; i++)
      {
         System.out.println("   " + usageString[i][0] + usageString[i][1]);
         System.out.println("      " + usageString[i][2]);
      }
      System.exit(0);
   }

   /** the main routine, parses the input switches, dumps the mission data */
   public static void main(String[] args)
   {
      DSPortAdapter adapter = null;

      boolean showHumidity = true;
      boolean showTemperature = true;
      boolean stopMission = false;
      boolean useOverdrive = false;
      byte[] readWritePass = null, readOnlyPass = null;

      try
      {

         if(args.length>0)
         {
            for(int i=0; i<args.length; i++)
            {
               String arg = args[i].toUpperCase();
               if(arg.equals(usageString[0][0].toUpperCase()))
               {
                  showTemperature = false;
               }
               else if(arg.equals(usageString[1][0].toUpperCase()))
               {
                  showHumidity = false;
               }
               else if(arg.equals(usageString[2][0].toUpperCase()))
               {
                  stopMission = true;
               }
               else if(arg.equals(usageString[3][0].toUpperCase()))
               {
                  useOverdrive = true;
               }
               else if(arg.indexOf(usageString[4][0].toUpperCase())==0)
               {
                  readWritePass = Convert.toByteArray(
                      arg.substring(usageString[4][0].length()));
               }
               else if(arg.indexOf(usageString[5][0].toUpperCase())==0)
               {
                  readOnlyPass = Convert.toByteArray(
                      arg.substring(usageString[5][0].length()));
               }
               else if(arg.equals("-H"))
               {
                  usage();
               }
               else
               {
                  System.out.println("bad argument: '" + args[i] + "'");
                  usage();
               }
            }
         }

         adapter = OneWireAccessProvider.getDefaultAdapter();

         adapter.beginExclusive(true);
         adapter.targetFamily(0x41);

         OneWireContainer41 owc = (OneWireContainer41)adapter.getFirstDeviceContainer();

         if(owc!=null)
         {
            System.out.println("Found " + owc.toString());
            if(useOverdrive)
            {
               System.out.println("setting speed as overdrive");
               owc.setSpeed(DSPortAdapter.SPEED_OVERDRIVE, true);
            }

            if(readWritePass!=null)
               owc.setContainerReadWritePassword(readWritePass, 0);
            if(readOnlyPass!=null)
               owc.setContainerReadOnlyPassword(readOnlyPass, 0);

            byte[] state = owc.readDevice(); //read to set container variables

            if(stopMission)
            {
               System.out.println("Stopping mission");
               boolean missionStopped = false;
               while(!missionStopped)
               {
                  try
                  {
                     if(!owc.isMissionRunning())
                     {
                        System.out.println("Mission is stopped");
                        missionStopped = true;
                     }
                     else
                     {
                        owc.stopMission();
                     }
                  }
                  catch(Exception e)
                  {;}
               }
            }

            boolean loadResults = false;
            while(!loadResults)
            {
               try
               {
                  System.out.println("loading mission results");
                  owc.loadMissionResults();
                  loadResults = true;
               }
               catch(Exception e)
               {
                  e.printStackTrace();
               }
            }

            System.out.println("Is Mission Running: " + owc.isMissionRunning());

            if(owc.isMissionSUTA())
               System.out.println("Start Upon Temperature Alarm: " +
                     (owc.isMissionWFTA()?"Waiting for Temperature Alarm":"Got Temperature Alarm, Mission Started"));

            System.out.println("Sample Rate: " + owc.getMissionSampleRate(0) + " secs");

            System.out.println("Mission Start Time: " +
                  (new java.util.Date(owc.getMissionTimeStamp(0))));

            System.out.println("Mission Sample Count: " + owc.getMissionSampleCount(0));

            System.out.println("Rollover Enabled: " + owc.isMissionRolloverEnabled());

            if(owc.isMissionRolloverEnabled())
            {
               System.out.println("Rollover Occurred: " + owc.hasMissionRolloverOccurred());

               if(owc.hasMissionRolloverOccurred())
               {
                  System.out.println("First Sample Timestamp: " +
                        (new java.util.Date(
                        owc.getMissionSampleTimeStamp(OneWireContainer41.TEMPERATURE_CHANNEL,0) |
                        owc.getMissionSampleTimeStamp(OneWireContainer41.DATA_CHANNEL,0))));
                  System.out.println("Total Mission Samples: " + owc.getMissionSampleCountTotal(0));
               }
            }

            System.out.println("Temperature Logging: " +
                  (!owc.getMissionChannelEnable(OneWireContainer41.TEMPERATURE_CHANNEL)?
                  "Disabled":
                  owc.getMissionResolution(OneWireContainer41.TEMPERATURE_CHANNEL) + " bit"));
            System.out.println("Temperature Low Alarm: " +
                  (!owc.getMissionAlarmEnable(OneWireContainer41.TEMPERATURE_CHANNEL, 0)?
                  "Disabled":
                  owc.getMissionAlarm(OneWireContainer41.TEMPERATURE_CHANNEL, 0) + "C ("
                  + (owc.hasMissionAlarmed(OneWireContainer41.TEMPERATURE_CHANNEL, 0)?
                  "ALARM)":"no alarm)")));
            System.out.println("Temperature High Alarm: " +
                  (!owc.getMissionAlarmEnable(OneWireContainer41.TEMPERATURE_CHANNEL, 1)?
                  "Disabled":
                  owc.getMissionAlarm(OneWireContainer41.TEMPERATURE_CHANNEL, 1) + "C ("
                  + (owc.hasMissionAlarmed(OneWireContainer41.TEMPERATURE_CHANNEL, 1)?
                  "ALARM)":"no alarm)")));

            System.out.println(owc.getMissionLabel(OneWireContainer41.DATA_CHANNEL) + " Logging: " +
                  (!owc.getMissionChannelEnable(OneWireContainer41.DATA_CHANNEL)?
                  "Disabled":
                  owc.getMissionResolution(OneWireContainer41.DATA_CHANNEL) + " bit"));
            System.out.println(owc.getMissionLabel(OneWireContainer41.DATA_CHANNEL) + " Low Alarm: " +
                  (!owc.getMissionAlarmEnable(OneWireContainer41.DATA_CHANNEL, 0)?
                  "Disabled":
                  owc.getMissionAlarm(OneWireContainer41.DATA_CHANNEL, 0) + "% RH ("
                  + (owc.hasMissionAlarmed(OneWireContainer41.DATA_CHANNEL, 0)?
                  "ALARM)":"no alarm)")));
            System.out.println(owc.getMissionLabel(OneWireContainer41.DATA_CHANNEL) + " High Alarm: " +
                  (!owc.getMissionAlarmEnable(OneWireContainer41.DATA_CHANNEL, 1)?
                  "Disabled":
                  owc.getMissionAlarm(OneWireContainer41.DATA_CHANNEL, 1) + "% RH ("
                  + (owc.hasMissionAlarmed(OneWireContainer41.DATA_CHANNEL, 1)?
                  "ALARM)":"no alarm)")));

            System.out.println("Total Device Samples: " + owc.getDeviceSampleCount());

            if(showTemperature)
            {
               System.out.println("Temperature Readings");
               if(!owc.getMissionChannelEnable(owc.TEMPERATURE_CHANNEL))
               {
                  System.out.println("Temperature Mission Not enabled");
               }
               else
               {
                  int dataCount = owc.getMissionSampleCount(owc.TEMPERATURE_CHANNEL);
                  System.out.println("SampleCount = " + dataCount);
                  for(int i=0; i<dataCount; i++)
                  {
                     System.out.println(owc.getMissionSample(owc.TEMPERATURE_CHANNEL, i));
                  }
               }
            }

            if(showHumidity)
            {
               System.out.println(
                  owc.getMissionLabel(owc.DATA_CHANNEL) + " Readings");
               if(!owc.getMissionChannelEnable(owc.DATA_CHANNEL))
               {
                  System.out.println(
                     owc.getMissionLabel(owc.DATA_CHANNEL)
                     + " Mission Not enabled");
               }
               else
               {
                  int dataCount = owc.getMissionSampleCount(owc.DATA_CHANNEL);
                  System.out.println("SampleCount = " + dataCount);
                  for(int i=0; i<dataCount; i++)
                  {
                    System.out.println(owc.getMissionSample(owc.DATA_CHANNEL, i));
                  }
               }
            }
         }
         else
         {
            System.out.println("No DS1922/DS2422 device found");
         }
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
      finally
      {
         if(adapter!=null)
            adapter.endExclusive();
      }   }

}