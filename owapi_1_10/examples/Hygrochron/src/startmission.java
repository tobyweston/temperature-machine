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
 * Starts a new mission on a DS1922/DS2422 temperature and A-D/Humidity
 * data-logger.  If this is a DS1922H, all data alarm values specified
 * will be considered humidity values and converted to the appropriate
 * A-D value before being written to the device.
 *
 * @version 1.00, 14 Aug, 2003
 * @author  shughes
 */
public class startmission
{
   /** usage string, all available command-line switches */
   static String[][] usageString = new String[][]
      {
         {"-sample", "N", "sample rate for mission in seconds, N=decimal (required)"},
         {"-tempBytes", "N", "resolution (in # of bytes) for temperature channel. 0-none 1-low 2-high"},
         {"-dataBytes", "N", "resolution (in # of bytes) for data/humidity channel. 0-none 1-low 2-high"},
         {"-tempAlarmHigh", "D", "value for temperature channel high alarm. D=double"},
         {"-tempAlarmLow", "D", "value for temperature channel low alarm. D=double"},
         {"-dataAlarmHigh", "D", "value for data/humidity channel high alarm. D=double"},
         {"-dataAlarmLow", "D", "value for data/humidity channel low alarm. D=double"},
         {"-startUponTempAlarm", "", "if present, mission SUTA bit is set"},
         {"-rollover", "", "if present, rollover is enabled"},
         {"-startDelay", "N", "number of minutes before mission should start, N=decimal"},
         {"-readWritePass", "H", "This is the read/write password to use for starting the mission, H=8 bytes of ascii-encoded hex"}
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

   /** the main routine, parses the input switches, starts the mission */
   public static void main(String[] args)
      throws Exception
   {
      DSPortAdapter adapter = null;

      boolean startUponTempAlarm = false;
      boolean rollover = false;
      int sampleRate = -1;
      int tempBytes = 1;
      int dataBytes = 1;
      int startDelay = 0;
      double tempAlarmHigh = -1, tempAlarmLow = -1;
      double dataAlarmHigh = -1, dataAlarmLow = -1;
      byte[] readWritePass = null;

      if(args.length==0)
         usage();

      for(int i=0; i<args.length; i++)
      {
         String arg = args[i].toUpperCase();
         if(arg.indexOf(usageString[0][0].toUpperCase())==0)
         {
            sampleRate = Integer.parseInt(
               arg.substring(usageString[0][0].length()));
         }
         else if(arg.indexOf(usageString[1][0].toUpperCase())==0)
         {
            tempBytes = Integer.parseInt(
               arg.substring(usageString[1][0].length()));
            if(tempBytes>2)
               tempBytes = 2;
            else if(tempBytes<0)
               tempBytes = 0;
         }
         else if(arg.indexOf(usageString[2][0].toUpperCase())==0)
         {
            dataBytes = Integer.parseInt(
               arg.substring(usageString[2][0].length()));
            if(dataBytes>2)
               dataBytes = 2;
            else if(dataBytes<0)
               dataBytes = 0;
         }
         else if(arg.indexOf(usageString[3][0].toUpperCase())==0)
         {

            tempAlarmHigh = toDouble(
               arg.substring(usageString[3][0].length()));
         }
         else if(arg.indexOf(usageString[4][0].toUpperCase())==0)
         {
            tempAlarmLow = toDouble(
               arg.substring(usageString[4][0].length()));
         }
         else if(arg.indexOf(usageString[5][0].toUpperCase())==0)
         {
            dataAlarmHigh = toDouble(
               arg.substring(usageString[5][0].length()));
         }
         else if(arg.indexOf(usageString[6][0].toUpperCase())==0)
         {
            dataAlarmLow = toDouble(
               arg.substring(usageString[5][0].length()));
         }
         else if(arg.equals(usageString[7][0].toUpperCase()))
         {
            startUponTempAlarm = true;
         }
         else if(arg.equals(usageString[8][0].toUpperCase()))
         {
            rollover = true;
         }
         else if(arg.indexOf(usageString[9][0].toUpperCase())==0)
         {
            startDelay = Integer.parseInt(
                arg.substring(usageString[9][0].length()));
         }
         else if(arg.indexOf(usageString[10][0].toUpperCase())==0)
         {
            readWritePass = Convert.toByteArray(
                arg.substring(usageString[10][0].length()));
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

      if(sampleRate==-1)
      {
         System.out.println("You must provide a sample rate");
         usage();
      }

      System.out.println("Sample Rate (seconds) = " + sampleRate);
      System.out.println("Temperature Bytes = " + tempBytes);
      System.out.println("Humidity Bytes = " + dataBytes);
      if(tempAlarmHigh!=-1)
         System.out.println("Temperature Alarm High = " + tempAlarmHigh);
      if(tempAlarmLow!=-1)
         System.out.println("Temperature Alarm Low = " + tempAlarmLow);
      if(dataAlarmHigh!=-1)
         System.out.println("Data Alarm High = " + dataAlarmHigh);
      if(dataAlarmLow!=-1)
         System.out.println("Data Alarm Low = " + dataAlarmLow);
      System.out.println("Start Upon Temp Alarm = " + startUponTempAlarm);
      System.out.println("Rollover Enabled = " + rollover);
      System.out.println("Start Delay (minutes) = " + startDelay);
      System.out.println();

      if(startUponTempAlarm && (tempAlarmHigh==-1 || tempAlarmLow==-1))
      {
         System.out.println("You selected a SUTA mission, but didn't specify high and low temp alarms");
         usage();
      }

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         adapter.beginExclusive(true);
         adapter.targetFamily(0x41);

         OneWireContainer41 owc = (OneWireContainer41)adapter.getFirstDeviceContainer();

         if(readWritePass!=null)
            owc.setContainerReadWritePassword(readWritePass, 0);

         byte[] state = owc.readDevice(); //read to set container variables

         if(owc!=null)
         {
            System.out.println("Found " + owc.toString());
            System.out.println("Stopping current mission, if there is one");
            if(owc.isMissionRunning())
            {
               owc.stopMission();
            }

            System.out.println("Starting a new mission");

            if(tempBytes==1)
               owc.setMissionResolution(0, owc.getMissionResolutions(0)[0]);
            else
               owc.setMissionResolution(0, owc.getMissionResolutions(0)[1]);

            if(dataBytes==1)
               owc.setMissionResolution(1, owc.getMissionResolutions(1)[0]);
            else
               owc.setMissionResolution(1, owc.getMissionResolutions(1)[1]);


            if(tempAlarmHigh!=-1)
            {
               owc.setMissionAlarm(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_HIGH,
                                    tempAlarmHigh);
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_HIGH,
                                    true);
            }
            else
            {
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_HIGH,
                                    false);
            }

            if(tempAlarmLow!=-1)
            {
               owc.setMissionAlarm(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_LOW,
                                    tempAlarmLow);
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_LOW,
                                    true);
            }
            else
            {
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.TEMPERATURE_CHANNEL,
                                    TemperatureContainer.ALARM_LOW,
                                    false);
            }

            if(dataAlarmHigh!=-1)
            {
               owc.setMissionAlarm(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_HIGH,
                                    dataAlarmHigh);
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_HIGH,
                                    true);
            }
            else
            {
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_HIGH,
                                    false);
            }

            if(dataAlarmLow!=-1)
            {
               owc.setMissionAlarm(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_LOW,
                                    dataAlarmLow);
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_LOW,
                                    true);
            }
            else
            {
               owc.setMissionAlarmEnable(
                                    OneWireContainer41.DATA_CHANNEL,
                                    ADContainer.ALARM_LOW,
                                    false);
            }

            owc.setStartUponTemperatureAlarmEnable(startUponTempAlarm);

            owc.startNewMission(sampleRate, startDelay, rollover,
               true, new boolean[]{tempBytes>0, dataBytes>0});

            System.out.println("Mission Started");
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
      }
   }

   // can't use Double.parseDouble on TINI
   // only allows 1 decimal place
   private static double toDouble(String dubbel)
   {
      int dot = dubbel.indexOf(".");
      if(dot<0)
         return Integer.parseInt(dubbel);

      int wholePart = 0;
      if(dot>0)
         wholePart = Integer.parseInt(dubbel.substring(0,dot));

      int fractionPart = Integer.parseInt(dubbel.substring(dot+1, 1));

      return (double)wholePart + fractionPart/10.0d;
   }
}
