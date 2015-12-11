
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

// FindiButtonsConsole.java
import java.util.*;
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;


//----------------------------------------------------------------------------

/** FindiButtonsConsole is a console application to view all of the iButtons
 *  on the currently available adapters.
 *
 *  @version    0.00, 28 Aug 2000
 *  @author     DS
 */
class FindiButtonsConsole
{

   //-------------------------------------------------------------------------

   /** Main for find iButtons test
    */
   public static void main (String args [])
   {
      System.out.println();
      System.out.println(
         "FindiButtonsConsole Java console application: Version 2.00");
      System.out.println();
      System.out.println("Adapter/Port\tiButton Type and ID\t\tDescription");
      System.out.println(
         "----------------------------------------------------------------");

      // enumerate through each of the adapter classes
      for (Enumeration adapter_enum = OneWireAccessProvider.enumerateAllAdapters();
              adapter_enum.hasMoreElements(); )
      {

         // get the next adapter DSPortAdapter
         DSPortAdapter adapter = ( DSPortAdapter ) adapter_enum.nextElement();

         // get the port names we can use and try to open, test and close each
         for (Enumeration port_name_enum = adapter.getPortNames();
                 port_name_enum.hasMoreElements(); )
         {

            // get the next packet
            String port_name = ( String ) port_name_enum.nextElement();

            try
            {

               // select the port
               adapter.selectPort(port_name);

               // verify there is an adaptered detected          
               if (adapter.adapterDetected())
               {
                  // added 8/29/2001 by SH
                  adapter.beginExclusive(true);
                  
                  // clear any previous search restrictions
                  adapter.setSearchAllDevices();
                  adapter.targetAllFamilies();

                  // enumerate through all the iButtons found
                  for (Enumeration ibutton_enum = adapter.getAllDeviceContainers();
                          ibutton_enum.hasMoreElements(); )
                  {

                     // get the next ibutton
                     OneWireContainer ibutton =
                        ( OneWireContainer ) ibutton_enum.nextElement();

                     System.out.println(
                        adapter.getAdapterName() + "/" + port_name + "\t"
                        + ibutton.getName() + "\t"
                        + ibutton.getAddressAsString() + "\t"
                        + ibutton.getDescription().substring(0, 25) + "...");
                  }
                  
                  // added 8/29/2001 by SH
                  adapter.endExclusive();
               }

               // free this port 
               adapter.freePort();
            }
            catch (Exception e){}
            ;
         }

         System.out.println();
      }

      System.exit(0);
   }
}
