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
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireSensor;
import com.dalsemi.onewire.container.HumidityContainer;
import java.util.Enumeration;
import java.io.FileInputStream;
import java.io.FileDescriptor;
import java.util.Vector;

/**
 * Minimal demo to read the 1-Wire Humidity Sensor
 *
 * @version    1.00, 31 August 2001
 * @author     DS
 */
public class HumidityTest
{
   public static void main(String args[])
   {
      try
      {
         Vector humidity_devices = new Vector(1);

         // get the default adapter and show header
         DSPortAdapter adapter = OneWireAccessProvider.getDefaultAdapter();
         System.out.println();
         System.out.println("Adapter: " + adapter.getAdapterName() + " Port: " + adapter.getPortName());
         System.out.println();
         System.out.println("Devices Found:");
         System.out.println("--------------");

         // get exclusive use of adapter/port
         adapter.beginExclusive(true);

         // find all devices
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         if(adapter.canFlex())
            adapter.setSpeed(adapter.SPEED_FLEX);

         // enumerate through all the 1-Wire devices found to find
         // containers that implement HumidityContainer
         for(Enumeration owd_enum = adapter.getAllDeviceContainers();
                         owd_enum.hasMoreElements(); )
         {
            OneWireContainer owd = (OneWireContainer)owd_enum.nextElement();
            System.out.print(owd.getAddressAsString());
            if (owd instanceof HumidityContainer)
            {
               humidity_devices.addElement(owd);
               System.out.println("  Humidity Sensor, Relative=" + ((HumidityContainer)owd).isRelative());
            }
            else
               System.out.println("  NOT Humidity Sensor");
         }

         if (humidity_devices.size() == 0)
            throw new Exception("No Humitiy devices found!");

         // display device found
         System.out.println();
         System.out.println("Hit ENTER to stop reading humidity");

         // loop and read RH or ENTER to quit
         FileInputStream keyboard = new FileInputStream(FileDescriptor.in);
         for (;;)
         {
            // read each RH temp found
            for (int i = 0; i < humidity_devices.size(); i++)
            {
               HumidityContainer hc = (HumidityContainer)humidity_devices.elementAt(i);
               byte[] state = hc.readDevice();
               hc.doHumidityConvert(state);
               System.out.println(((OneWireContainer)hc).getAddressAsString() +
                    " humidity = " + hc.getHumidity(state) + "%");
            }

            // check for ENTER
            if (keyboard.available() != 0)
               break;
         }

         // end exclusive use of adapter
         adapter.endExclusive();
         // free port used by adapter
         adapter.freePort();
      }
      catch(Exception e)
      {
         System.out.println(e);
      }

      return;
   }
}
