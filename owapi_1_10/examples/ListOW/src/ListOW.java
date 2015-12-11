
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

import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import java.util.Enumeration;


/**
 * Minimal demo to list device found on default 1-Wire port
 *
 * @version    0.00, 28 August 2000
 * @author     DS
 */
public class ListOW
{

   /**
    * Method main
    *
    *
    * @param args
    *
    */
   public static void main (String args [])
   {
      OneWireContainer owd;

      try
      {

         // get the default adapter  
         DSPortAdapter adapter = OneWireAccessProvider.getDefaultAdapter();

         System.out.println();
         System.out.println("Adapter: " + adapter.getAdapterName()
                            + " Port: " + adapter.getPortName());
         System.out.println();

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // enumerate through all the 1-Wire devices found
         for (Enumeration owd_enum = adapter.getAllDeviceContainers();
                 owd_enum.hasMoreElements(); )
         {
            owd = ( OneWireContainer ) owd_enum.nextElement();

            System.out.println(owd.getAddressAsString());
         }

         // end exclusive use of adapter
         adapter.endExclusive();

         // free port used by adapter
         adapter.freePort();
      }
      catch (Exception e)
      {
         System.out.println(e);
      }

      return;
   }
}
