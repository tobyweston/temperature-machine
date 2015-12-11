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
 * Thread class to monitor a sensor 
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class DeviceMonitorSensor
                   extends Thread
{
   //--------
   //-------- Variables
   //--------

   /** Frame that is displaying the sensor */
   private DeviceFrameSensor sensorFrame;

   /** the tagged device */
   protected TaggedDevice sensor;

   /** Last poll delay value in seconds */
   protected int lastPollDelay;

   /** Counter used to calculate the next sensor read */
   protected int currentSecondCount;

   /** 1-Wire adapter used to access the sensor */
   protected DSPortAdapter adapter;

   //--------
   //-------- Constructors
   //--------

   /**
    * Don't allow anyone to instantiate without providing device
    */
   private DeviceMonitorSensor ()
   {
   }

   /**
    * Create an sensor monitor.
    *
    * @param  dev Tagged device to monitor
    * @param  logFile file name to log to
    */
   public DeviceMonitorSensor(TaggedDevice dev, String logFile)
   {
      // get ref to the contact device
      sensor = dev;

      // create the Frame that will display this device
      sensorFrame = new DeviceFrameSensor(dev,logFile);

      // init
      lastPollDelay = 0;
      currentSecondCount = 0;

      adapter = sensor.getDeviceContainer().getAdapter();
      
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
         // check for read key press
         if (sensorFrame.getReadButtonClick())
            makeSensorReading();

         // check for new polldelay rate
         if (lastPollDelay != sensorFrame.getPollDelay())
         {
            lastPollDelay = sensorFrame.getPollDelay();
            currentSecondCount = 0;
         }
         
         // check if time to do a poll reading
         if ((lastPollDelay != 0) && (currentSecondCount >= lastPollDelay))
         {
            makeSensorReading();
            currentSecondCount = 0;
         }

         // count the seconds
         currentSecondCount++;

         // sleep for 1 second
         try
         {
           Thread.sleep(1000);
         }
         catch (InterruptedException e){}
      }
   }

   /**
    * Makes a sensor reading 
    */
   public void makeSensorReading()
   {
      try
      {
         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // open path to the device
         sensor.getOWPath().open();

         // read the sensor update 
         String reading = ((TaggedSensor)sensor).readSensor();
         sensorFrame.setSensorLabel(reading);

         // close the path to the device
         sensor.getOWPath().close();

         // display time of last reading
          sensorFrame.showTime("Last Reading: ");

         // log if enabled
         if ((reading.length() > 0) && (sensorFrame.isLogChecked()))
            sensorFrame.log(reading);
      }
      catch (OneWireException e)
      {
         System.out.println(e); 
         // log if enabled
         if (sensorFrame.isLogChecked())
            sensorFrame.log(e.toString());
      }
      finally
      {
         // end exclusive use of adapter
         adapter.endExclusive();
      }
   }   
}
