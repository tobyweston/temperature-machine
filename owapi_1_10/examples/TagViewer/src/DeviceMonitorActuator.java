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

import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/** 
 * Thread class to monitor an actuator 
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class DeviceMonitorActuator
                   extends Thread
{
   //--------
   //-------- Variables
   //--------

   /** Frame that is displaying the actuator */  
   private DeviceFrameActuator actuatorFrame;

   /** The actual tagged device */
   protected TaggedDevice actuator;
 
   /** Last selection to see if it has changed */
   protected String lastSelection;

   /** 1-Wire adapter used to access the actuator */
   protected DSPortAdapter adapter;

   /** Did initialization flag */
   protected boolean didInit;

   //--------
   //-------- Constructors
   //--------

   /**
    * Don't allow anyone to instantiate without providing device
    */
   private DeviceMonitorActuator ()
   {
   }

   /**
    * Create an actuator monitor.
    *
    * @param  dev Tagged device to monitor
    * @param  logFile file name to log to
    */
   public DeviceMonitorActuator(TaggedDevice dev, String logFile)
   {
      // get ref to the contact device
      actuator = dev;

      // create the Frame that will display this device
      actuatorFrame = new DeviceFrameActuator(dev,logFile);
      
      // hide the read items since this is an actuator
      actuatorFrame.hideReadItems();

      // get adapter ref
      adapter = actuator.getDeviceContainer().getAdapter();
      
      // init last selection
      lastSelection = new String("");
      didInit = false;

      // start up this service thread
      start();
   }

   //--------
   //-------- Methods
   //--------

   /**
    * Device monitor run method
    */
   public void run ()
   {
      // run loop
      for (;;)
      {
         // check if did init
         if (!didInit)    
         {     
            // initialize the actuator
            didInit = initialize();
         }

         // check for change in selection
         String new_selection = actuatorFrame.getSelection();

         // check if these is a selection and it is different
         if (new_selection != null)
         {
            if (!lastSelection.equals(new_selection))
            {
               setSelection(new_selection);
               lastSelection = new_selection;
            }
         }

         // sleep for 200 milliseconds
         try
         {
           Thread.sleep(200);
         }
         catch (InterruptedException e){}
      }
   }

   /**
    * Sets the selection in the device to match the window.
    *
    * @param  selection string 
    */
   public void setSelection(String selection)
   {
      try
      {
         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // open path to the device
         actuator.getOWPath().open();

         // set the actuator to the selection 
         ((TaggedActuator)actuator).setSelection(selection);

         // close the path to the device
         actuator.getOWPath().close();

         // display time of last reading
         actuatorFrame.showTime("Action Serviced at: ");

         // log if enabled
         if (actuatorFrame.isLogChecked())
            actuatorFrame.log(selection);
      }
      catch (OneWireException e)
      {
         System.out.println(e); 
         // log exception if enabled
         if (actuatorFrame.isLogChecked())
            actuatorFrame.log(e.toString());
      }
      finally
      {
         // end exclusive use of adapter
         adapter.endExclusive();
      }
   }   

   /**
    * Initialize the actuator
    *
    * @return 'true' if initialization was successful 
    */
   public boolean initialize()
   {
      boolean rslt = false;

      try
      {
         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // open path to the device
         actuator.getOWPath().open();

         // initialize the actuator 
         ((TaggedActuator)actuator).initActuator();

         // close the path to the device
         actuator.getOWPath().close();

         // display time of last reading
         actuatorFrame.showTime("Initialized at: ");

         rslt = true;
      }
      catch (OneWireException e)
      {
         System.out.println(e); 
         // log exception if enabled
         if (actuatorFrame.isLogChecked())
            actuatorFrame.log(e.toString());
      }
      finally
      {
         // end exclusive use of adapter
         adapter.endExclusive();
      }

      return rslt;
   }   
}
