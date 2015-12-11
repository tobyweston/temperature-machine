
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
public class ReadSwitch
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
      System.out.println("Switch Container Demo\r\n");
      System.out.println("Usage: ");
      System.out.println("   java ReadSitch ADAPTER_PORT\r\n");
      System.out.println(
         "ADAPTER_PORT is a String that contains the name of the");
      System.out.println(
         "adapter you would like to use and the port you would like");
      System.out.println("to use, for example: ");
      System.out.println("   java ReadSwitch {DS1410E}_LPT1\r\n");
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

         boolean         isSwitchContainer = false;
         SwitchContainer sc                = null;

         try
         {
            sc                = ( SwitchContainer ) owc;
            isSwitchContainer = true;
         }
         catch (Exception e)
         {
            sc                = null;
            isSwitchContainer = false;   //just to reiterate
         }

         if (isSwitchContainer)
         {
            System.out.println("= This device is a " + owc.getName());
            System.out.println("= Also known as a "
                               + owc.getAlternateNames());
            System.out.println("=");
            System.out.println("= It is a Switch Container");
            if (sc.hasActivitySensing())
               sc.clearActivity();

            byte[]  state    = sc.readDevice();
            int     channels = sc.getNumberChannels(state);
            boolean activity = sc.hasActivitySensing();
            boolean level    = sc.hasLevelSensing();
            boolean smart    = sc.hasSmartOn();

            System.out.println("= This device has " + channels + " channel"
                               + (channels > 1 ? "s"
                                               : ""));
            System.out.println("= It " + (activity ? "has"
                                                   : "does not have") + " activity sensing abilities");
            System.out.println("= It " + (level ? "has"
                                                : "does not have") + " level sensing abilities");
            System.out.println("= It " + (smart ? "is"
                                                : "is not") + " smart-on capable");

            for (int ch = 0; ch < channels; ch++)
            {
               System.out.println("======================");
               System.out.println("=   Channel " + ch + "        =");
               System.out.println("=--------------------=");

               boolean latchstate = sc.getLatchState(ch, state);

               System.out.println("= State " + (latchstate ? "ON "
                                                           : "OFF") + "          =");

               if (level)
               {
                  boolean sensedLevel = sc.getLevel(ch, state);

                  System.out.println("= Level " + (sensedLevel ? "HIGH"
                                                               : "LOW ") + "         =");
               }

               if (activity)
               {
                  boolean sensedActivity = sc.getSensedActivity(ch, state);

                  System.out.println("= Activity " + (sensedActivity ? "YES"
                                                                     : "NO ") + "       =");
               }

               System.out.println("= Toggling switch... =");

               try
               {
                  Thread.sleep(500);
               }
               catch (Exception e)
               {

                  /*drain it*/
               }

               sc.setLatchState(ch, !latchstate, smart, state);
               sc.writeDevice(state);

               state = sc.readDevice();

               if (latchstate == sc.getLatchState(ch, state))
               {
                  System.out.println("= Toggle Failed      =");
               }
               else
               {
                  try
                  {
                     Thread.sleep(500);
                  }
                  catch (Exception e)
                  {

                     /*drain it*/
                  }

                  System.out.println("= Toggling back...   =");
                  sc.setLatchState(ch, latchstate, smart, state);
                  sc.writeDevice(state);
               }
            }
         }
         else
         {
            System.out.println("= This device is not a Switch device.");
            System.out.println("=");
            System.out.println("=");
         }

         next = access.findNextDevice();
      }
   }
}
