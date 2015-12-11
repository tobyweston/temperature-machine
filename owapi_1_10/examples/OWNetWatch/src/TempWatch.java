
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

import com.dalsemi.onewire.utils.OWPath;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.container.OneWireContainer;
import java.lang.InterruptedException;


/**
 * Thread to monitor a temperature device on a path in a
 * a complex network.
 *
 * @version    0.00, 18 September 2000
 * @author     DS
 */
public class TempWatch
   extends Thread
{

   //--------
   //-------- Variables 
   //--------
   private boolean              keepRunning = true;
   private OWPath               path;
   private TemperatureContainer tc;
   private String               address;
   private DSPortAdapter        adapter;
   private OneWireContainer     owc;

   //--------
   //-------- Constructor
   //--------

   /**
    * Create TemperatureContainer Watcher
    *
    * @param tc TemperatureContainer to read
    * @param path path to get to the device in a complex
    *        network
    */
   public TempWatch (TemperatureContainer tc, OWPath path)
   {
      this.tc   = tc;
      this.path = path;

      // extract out the address and adapter
      owc     = ( OneWireContainer ) tc;
      address = owc.getAddressAsString();
      adapter = owc.getAdapter();
   }

   /**
    * Stop this watcher thread
    */
   public void killWatch ()
   {

      // clear the flag to stop the thread
      synchronized (this)
      {
         keepRunning = false;
      }

      // wait for thread death
      while (isAlive())
      {
         try
         {
            sleep(20);
         }
         catch (InterruptedException e)
         {

            // DRAIN
         }
      }
   }

   /**
    * Work done by thread
    */
   public void run ()
   {
      while (keepRunning)
      {
         try
         {

            // get exclusive use of port
            adapter.beginExclusive(true);

            // open a path to the temp device
            path.open();

            // check if present
            if (owc.isPresent())
            {

               // read the temp device
               byte[] state = tc.readDevice();

               tc.doTemperatureConvert(state);

               state = tc.readDevice();

               System.out.println("Temperature of " + address + " is "
                                  + tc.getTemperature(state) + " C");
            }
            else
            {
               System.out.println("Device " + address
                                  + " not present so stopping thread");

               synchronized (this)
               {
                  keepRunning = false;
               }
            }

            // close the path to the device
            path.close();

            // release exclusive use of port
            adapter.endExclusive();

            // sleep for a while
            if (keepRunning)
            {
               sleep(2000);
            }
         }
         catch (OneWireIOException e)
         {

            // DRAIN
         }
         catch (OneWireException e)
         {

            // DRAIN
         }
         catch (InterruptedException e)
         {

            // DRAIN
         }
      }
   }
}
