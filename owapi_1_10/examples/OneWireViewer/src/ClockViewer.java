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

import javax.swing.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.ClockContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;

/**
 * A <code>ClockContainer</code> Viewer for integration in the OneWireViewer.
 * All devices that implement <code>ClockContainer</code> are supported by
 * this viewer.  This viewer displays (and polls) the current time as
 * reported by the 1-Wire Real-time clock device.
 *
 * @author SH
 * @version 1.00
 */
public class ClockViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "ClockViewer";
   private static final String strTab = "Clock";
   private static final String strTip = "Shows time from clock device";
   private static final String naString = "N/A";

   /* container variables */
   private ClockContainer container  = null;
   private TaggedDevice taggedDevice = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = { "Current Time ",
                                  "Difference from PC Time " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 2;
   private static final int TIME = 0;
   private static final int DIFF = 1;

   private boolean syncButtonClicked = false;
   private boolean haltButtonClicked = false;

   /**
    * Creates a new ClockViewer.
    */
   public ClockViewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 2;

      // Info panel
      JPanel featurePanel = new JPanel(new GridLayout(TOTAL_FEATURES, 2, 3, 3));
      JScrollPane featureScroll = new JScrollPane(featurePanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      featureScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Info"));

      lblFeatureHdr = new JLabel[TOTAL_FEATURES];
      lblFeature = new JLabel[TOTAL_FEATURES];
      for(int i=0; i<TOTAL_FEATURES; i++)
      {
         lblFeatureHdr[i] = new JLabel(strHeader[i], JLabel.RIGHT);
         lblFeatureHdr[i].setOpaque(true);
         lblFeatureHdr[i].setFont(fontBold);
         lblFeatureHdr[i].setForeground(Color.black);
         lblFeatureHdr[i].setBackground(Color.lightGray);

         lblFeature[i] = new JLabel("", JLabel.LEFT);
         lblFeature[i].setOpaque(true);
         lblFeature[i].setFont(fontPlain);
         lblFeature[i].setForeground(Color.black);
         lblFeature[i].setBackground(Color.lightGray);

         featurePanel.add(lblFeatureHdr[i]);
         featurePanel.add(lblFeature[i]);
      }

      // Config panel
      JPanel configPanel = new JPanel(new FlowLayout());
      configPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Config"));

      JButton syncClock = new JButton("Synchronize Clock to PC Time");
      syncClock.addActionListener( new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               syncButtonClicked = true;
            }
         }
      );
      configPanel.add(syncClock);
      JButton haltClock = new JButton("Halt Real-Time Clock");
      haltClock.addActionListener( new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               haltButtonClicked = true;
            }
         }
      );
      configPanel.add(haltClock);

      add(featureScroll, BorderLayout.NORTH);
      add(configPanel, BorderLayout.CENTER);

      clearContainer();
   }

   /**
    * Checks if this viewer supports the supplied TaggedDevice.
    *
    * @param td - TaggedDevice to check for viewer support.
    *
    * @return 'true' if this viewer supports the provided
    *   TaggedDevice.
    */
   public boolean containerSupported(TaggedDevice td)
   {
      // not yet supported as a tagged device
      return false;
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      // not yet supported as a tagged device
      return;
   }

   /**
    * Checks if this viewer supports the supplied container.
    *
    * @param owc - container to check for viewer support.
    *
    * @return 'true' if this viewer supports the provided
    *   container.
    */
   public boolean containerSupported(OneWireContainer owc)
   {
      return (owc instanceof ClockContainer);
   }

   /**
    * Sets the container for this viewer.
    *
    * @param owc OneWireContainer of this viewer
    */
   public void setContainer(OneWireContainer owc)
   {
      // ensure that the container was cleared previously
      if(this.adapter!=null || this.container!=null || this.romID!=null)
         clearContainer();

      if(owc!=null)
      {
         synchronized(syncObj)
         {
            this.adapter = owc.getAdapter();
            this.container = (ClockContainer)owc;
            this.romID = owc.getAddressAsString();
            this.syncButtonClicked = false;
         }
         setStatus(VERBOSE, "Performing first read. . .");
      }
   }

   /**
    * Clears the reference to the device container.
    */
   public void clearContainer()
   {
      synchronized(syncObj)
      {
         this.adapter = null;
         this.container = null;
         this.romID = null;
         this.syncButtonClicked = false;
         this.haltButtonClicked = false;
      }
      setStatus(VERBOSE, "No Device");
   }

   /**
    * Returns <code>true</code> if Viewer still has pending tasks it must
    * complete.
    *
    * @return <code>true</code> if Viewer still has pending tasks it must
    * complete.
    */
    public boolean isBusy()
    {
       //return hasRunTasks();
       // should never have any tasks enqueued, but not serviced
       return false;
    }

   /**
    * 'run' method that is called continuously to service
    * GUI interaction, such as button click events.  Also
    * performs one-time setup of viewer.
    */
   public void run()
   {
      DSPortAdapter l_adapter = null;
      ClockContainer l_container = null;
      String l_romID = null;
      synchronized(syncObj)
      {
         l_adapter = this.adapter;
         l_container = this.container;
         l_romID = this.romID;
      }

      if(l_adapter!=null)
      {
         if(this.syncButtonClicked)
         {
            setStatus(VERBOSE, "Synching clock device...");
            try
            {
               l_adapter.beginExclusive(true);
               byte[] state = l_container.readDevice();
               l_container.setClock((new Date()).getTime(), state);
               if(!l_container.isClockRunning(state))
                  l_container.setClockRunEnable(true, state);
               l_container.writeDevice(state);
               this.syncButtonClicked = false;
               setStatus(VERBOSE, "Done synching.");
            }
            catch(Exception e)
            {
               setStatus(ERROR, "Error reading device! "+e.toString());
            }
            finally
            {
               l_adapter.endExclusive();
            }
         }
         else if(this.haltButtonClicked)
         {
            setStatus(VERBOSE, "Halting clock device...");
            try
            {
               l_adapter.beginExclusive(true);
               byte[] state = l_container.readDevice();
               l_container.setClockRunEnable(false, state);
               l_container.writeDevice(state);
               this.haltButtonClicked = false;
               setStatus(VERBOSE, "Done halting.");
            }
            catch(Exception e)
            {
               setStatus(ERROR, "Error reading device! "+e.toString());
            }
            finally
            {
               l_adapter.endExclusive();
            }
         }
      }
   }

   /**
    * 'poll' method that is called at the current polling rate.
    * Read and update the display of the device status.
    */
   public void poll()
   {
      DSPortAdapter l_adapter = null;
      ClockContainer l_container = null;
      String l_romID = null;
      synchronized(syncObj)
      {
         l_adapter = this.adapter;
         l_container = this.container;
         l_romID = this.romID;
      }

      if(l_adapter!=null)
      {
         setStatus(VERBOSE, "Polling device...");
         try
         {
            l_adapter.beginExclusive(true);
            byte[] state = l_container.readDevice();
            long PCTime = (new Date()).getTime();
            if(!l_container.isClockRunning(state))
            {
               lblFeature[TIME].setText(" Clock is not running");
               lblFeature[DIFF].setText(" N/A");
            }
            else
            {
               long time = l_container.getClock(state);
               lblFeature[TIME].setText(" " + new Date(time).toString());
               long diff = Math.abs(time - PCTime)/1000;
               lblFeature[DIFF].setText(" " + diff + " seconds");
            }
            setStatus(VERBOSE, "Done polling.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error reading device! "+e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * Gets the string that represents this Viewer's title
    *
    * @return viewer's title
    */
   public String getViewerTitle()
   {
      return strTab;
   }

   /**
    * Gets the string that represents this Viewer's description
    *
    * @return viewer's description
    */
   public String getViewerDescription()
   {
      return strTip;
   }

   /**
    * Create a complete clone of this viewer, including reference to
    * current container.  Used to display viewer in new window.
    */
   public Viewer cloneViewer()
   {
      ClockViewer cv = new ClockViewer();
      cv.setContainer((OneWireContainer)this.container);
      return cv;
   }
}