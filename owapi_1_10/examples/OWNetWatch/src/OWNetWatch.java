
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

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.monitor.*;
import java.util.Enumeration;
import java.util.Vector;


/**
 * Minimal demo to monitor a complex network and read temperatures
 *
 * @version    0.00, 28 August 2000
 * @author     DS
 */
public class OWNetWatch
   implements DeviceMonitorEventListener
{

   /**
    * Main for the OWNetWatch Demo
    *
    * @param args command line arguments
    */
   public static void main (String args [])
   {
      OneWireContainer owd;
      int              delay;

      try
      {

         // get the default adapter  
         DSPortAdapter adapter = OneWireAccessProvider.getDefaultAdapter();

         System.out.println();
         System.out.println("Adapter: " + adapter.getAdapterName()
                            + " Port: " + adapter.getPortName());
         System.out.println();

         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // create the watcher with this adapter
         OWNetWatch nw = new OWNetWatch(adapter);

         // sleep for the specified time
         if (args.length >= 1)
            delay = Integer.decode(args [0]).intValue();
         else
            delay = 20000;

         System.out.println("Monitor run for: " + delay + "ms");
         Thread.sleep(delay);

         // clean up
         System.out.println("Done with monitor run, now cleanup threads");
         nw.killNetWatch();

         // free port used by adapter
         adapter.freePort();
      }
      catch (Exception e)
      {
         System.out.println(e);
      }

      return;
   }

   //--------
   //-------- Variables 
   //--------

   /** Network Monitor intance */
   private NetworkDeviceMonitor nm;

   /** Vector of temperature watches, used in cleanup */
   private Vector watchers;

   //--------
   //-------- Constructor
   //--------

   /**
    * Create a 1-Wire Network Watcher
    *
    * @param  adapter for 1-Wire Network to monitor
    */
   public OWNetWatch (DSPortAdapter adapter)
   {

      // create vector to keep track of temperature watches
      watchers = new Vector(1, 1);

      // create a network monitor
      nm = new NetworkDeviceMonitor(adapter);

      // add this to the event listers
      nm.addDeviceMonitorEventListener(this);

      // start the monitor
      Thread t = new Thread(nm);
      t.start();
   }

   /**
    * Clean up the threads
    */
   public void killNetWatch ()
   {
      TempWatch tw;

      // kill the network monitor
      nm.killMonitor();

      // kill the temp watchers
      for (int i = 0; i < watchers.size(); i++)
      {
         tw = ( TempWatch ) watchers.elementAt(i);

         tw.killWatch();
      }
   }

   /**
    * Arrival event as a NetworkMonitorEventListener
    *
    * @param nme NetworkMonitorEvent add
    */
   public void deviceArrival (DeviceMonitorEvent dme)
   {
      for(int i=0; i<dme.getDeviceCount(); i++)
      {
         System.out.println("ADD: " + dme.getPathForContainerAt(i)
                            + dme.getAddressAsStringAt(i));
   
         // if new devices is a TemperatureContainter the start a thread to read it
         OneWireContainer owc = dme.getContainerAt(i);
   
         if (owc instanceof TemperatureContainer)
         {
            TempWatch tw = new TempWatch(( TemperatureContainer ) owc,
                                         dme.getPathForContainerAt(i));
   
            tw.start();
   
            // add to vector for later cleanup
            watchers.addElement(tw);
         }
      }
   }

   /**
    * Depart event as a NetworkMonitorEventListener
    *
    * @param nme NetworkMonitorEvent depart
    */
   public void deviceDeparture (DeviceMonitorEvent dme)
   {
      for(int i=0; i<dme.getDeviceCount(); i++)
      {
         System.out.println("REMOVE: " + dme.getPathForContainerAt(i)
                            + dme.getAddressAsStringAt(i));
   
         // it would be nice if I killed the temp watcher
      }
   }

   /**
    * Exception event as a NetworkMonitorEventListener
    *
    * @param ex Exception
    */
   public void networkException (DeviceMonitorException ex)
   {
      if (ex.getException() instanceof OneWireIOException)
         System.out.print(".IO.");
      else
         System.out.print(ex);
      ex.getException().printStackTrace();
   }
}
