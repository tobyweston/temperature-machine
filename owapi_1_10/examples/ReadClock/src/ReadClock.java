
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

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import java.util.Vector;
import java.io.*;
import com.dalsemi.onewire.utils.CRC16;
import java.util.*;


/* author KLA */
public class ReadClock
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
      System.out.println("Clock Container Demo\r\n");
      System.out.println("Usage: ");
      System.out.println("   java ReadClock ADAPTER_PORT\r\n");
      System.out.println(
         "ADAPTER_PORT is a String that contains the name of the");
      System.out.println(
         "adapter you would like to use and the port you would like");
      System.out.println("to use, for example: ");
      System.out.println("   java ReadClock {DS1410E}_LPT1\r\n");
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
      access.targetAllFamilies();
      access.beginExclusive(true);
      access.reset();
      access.setSearchAllDevices();

      boolean next = access.findFirstDevice();

      if (!next)
      {
         System.out.println("Could not find any iButtons!");

         return;
      }

      while (next)
      {
         OneWireContainer owc = access.getDeviceContainer();

         System.out.println(
            "====================================================");
         System.out.println("= Found One Wire Device: "
                            + owc.getAddressAsString() + "          =");
         System.out.println(
            "====================================================");
         System.out.println("=");

         boolean        isClockContainer = false;
         ClockContainer cc               = null;

         try
         {
            cc               = ( ClockContainer ) owc;
            isClockContainer = true;
         }
         catch (Exception e)
         {
            cc               = null;
            isClockContainer = false;   //just to reiterate
         }

         if (isClockContainer)
         {
            System.out.println("= This device is a " + owc.getName());
            System.out.println("= Also known as a "
                               + owc.getAlternateNames());
            System.out.println("=");
            System.out.println("= It is a Clock Container");

            byte[] state      = cc.readDevice();
            long   resolution = cc.getClockResolution();

            System.out.println("= The clock resolution is " + resolution
                               + " milliseconds");

            boolean alarm = cc.hasClockAlarm();

            System.out.println("= This clock " + (alarm ? "does"
                                                        : "does not") + " have a clock alarm");

            long rtc = cc.getClock(state);

            System.out.println("= Clock raw time: " + rtc);
            System.out.println("= Readable clock time: " + new Date(rtc));

            boolean alarmenabled = false;

            if (alarm)
            {
               long aclock = cc.getClockAlarm(state);

               alarmenabled = cc.isClockAlarmEnabled(state);

               boolean alarming = cc.isClockAlarming(state);

               System.out.println("= Raw clock alarm: " + aclock);
               System.out.println("= Readable clock alarm: "
                                  + new Date(aclock));
               System.out.println("= The alarm is" + (alarmenabled ? ""
                                                                   : " not") + " enabled");
               System.out.println("= The alarm is" + (alarming ? ""
                                                               : " not") + " alarming");
            }

            boolean running    = cc.isClockRunning(state);
            boolean candisable = cc.canDisableClock();

            System.out.println("= The clock is" + (running ? ""
                                                           : " not") + " running");
            System.out.println("= The clock can" + (candisable ? ""
                                                               : " not") + " be disabled");
            cc.setClock(System.currentTimeMillis(), state);

            if (alarm)
            {
               try
               {
                  cc.setClockAlarm(System.currentTimeMillis() + 1000 * 60,
                                   state);
                  System.out.println("= Set the clock alarm");
               }
               catch (Exception e)
               {
                  System.out.println("= Could not set the clock alarm");
               }

               cc.setClockAlarmEnable(!alarmenabled, state);
               System.out.println("= " + (alarmenabled ? "Disabled"
                                                       : "Enabled") + " the clock alarm");
            }

            if (candisable)
            {
               cc.setClockRunEnable(!running, state);
               System.out.println("= " + (running ? "Disabled"
                                                  : "Enabled") + " the clock oscillator");
            }

            System.out.println("= Writing device state...");

            try
            {
               cc.writeDevice(state);
               System.out.println("= Successfully wrote device state");
            }
            catch (Exception e)
            {
               System.out.println("= Failed to write device state: "
                                  + e.toString());
            }
         }
         else
         {
            System.out.println("= This device is not a Clock device.");
            System.out.println("=");
            System.out.println("=");
         }

         next = access.findNextDevice();
      }
   }
}
