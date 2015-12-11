
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


public class ReadDigPot
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
      System.out.println("Potentiometer Container Demo\r\n");
      System.out.println("Usage: ");
      System.out.println("   java ReadDigPot ADAPTER_PORT\r\n");
      System.out.println(
         "ADAPTER_PORT is a String that contains the name of the");
      System.out.println(
         "adapter you would like to use and the port you would like");
      System.out.println("to use, for example: ");
      System.out.println("   java ReadDigPot {DS1410E}_LPT1\r\n");
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

         boolean                isPotContainer = false;
         PotentiometerContainer pc             = null;

         try
         {
            pc             = ( PotentiometerContainer ) owc;
            isPotContainer = true;
         }
         catch (Exception e)
         {
            pc             = null;
            isPotContainer = false;   //just to reiterate
         }

         if (isPotContainer)
         {
            System.out.println("= This device is a " + owc.getName());
            System.out.println("= Also known as a "
                               + owc.getAlternateNames());
            System.out.println("=");
            System.out.println("= It is a Potentiometer Container");

            byte[]  state       = pc.readDevice();
            boolean charge      = pc.isChargePumpOn(state);
            int     wiper       = pc.getCurrentWiperNumber(state);
            int     position    = pc.getWiperPosition();
            boolean linear      = pc.isLinear(state);
            int     numPots     = pc.numberOfPotentiometers(state);
            int     numSettings = pc.numberOfWiperSettings(state);
            int     resistance  = pc.potentiometerResistance(state);

            System.out.println("=");
            System.out.println("= Charge pump is " + (charge ? "ON"
                                                             : "OFF"));
            System.out.println("= This device has " + numPots
                               + " potentiometer" + ((numPots > 1) ? "s"
                                                                   : ""));
            System.out.println("= This device has a " + (linear ? "linear"
                                                                : "logarithmic") + " potentiometer");
            System.out.println("= It uses " + numSettings
                               + " potentiometer wiper settings");
            System.out.println("= The potentiometer resistance is "
                               + resistance + " kOhms");
            System.out.println("= CURRENT WIPER NUMBER  : " + wiper);
            System.out.println("= CURRENT WIPER POSITION: " + position);
            System.out.println("= Trying to toggle the charge pump...");
            pc.setChargePump(!charge, state);
            pc.writeDevice(state);

            state = pc.readDevice();

            if (charge == pc.isChargePumpOn(state))
               System.out.println(
                  "= Could not toggle charge pump.  Must have external power supplied.");
            else
               System.out.println("= Toggled charge pump successfully");

            int newwiper = ( int ) ((System.currentTimeMillis() & 0x0ff00)
                                    >> 8);

            pc.setWiperPosition(newwiper);
            System.out.println("= Setting wiper position to " + (newwiper));
            System.out.println("= CURRENT WIPER POSITION: "
                               + pc.getWiperPosition());
         }
         else
         {
            System.out.println(
               "= This device is not a potentiometer device.");
            System.out.println("=");
            System.out.println("=");
         }

         next = access.findNextDevice();
      }
   }
}
