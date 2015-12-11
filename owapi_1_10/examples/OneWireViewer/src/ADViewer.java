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
import java.awt.*;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.ADContainer;
import com.dalsemi.onewire.container.OneWireContainer41;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.utils.OWPath;

/**
 * A <code>ADContainer</code> Viewer for integration in the OneWireViewer.
 * All devices that implement <code>ADContainer</code> are supported by
 * this viewer.  This viewer creates a list of A-to-D elements (actual number
 * of A-to-D channels which the device supports) and allow the user to see the
 * current value of the A-to-D conversion on each channel.
 *
 * @author SH
 * @version 1.00
 */
public class ADViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "ADViewer";
   private static final String strTab = "A to D";
   private static final String strTip = "Shows voltage readings from AtoD device.";
   private static final String naString = "N/A";

   /* container variables */
   private ADContainer container  = null;
   private TaggedDevice taggedDevice = null;
   private OWPath pathToDevice = null;

   /* maintain last Temperature value read, only update when it changes */
   private double lastTemperatureRead = Double.NaN;

   /* visual components */
   private AdjustableMeter meter = null;
   private double newResolution = Double.NaN;
   private JPanel elementPanel = null;
   private ADElement[] elements = null;
   private boolean[] includeAD = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = { "Number of Channels ",
                                  "Supports Multi-Channel Read? ",
                                  "Has High/Low AD Alarms? " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 3;
   private static final int NUM_CHANS = 0;
   private static final int MULTI_CHAN = 1;
   private static final int HAS_ALARMS = 2;

   private volatile boolean pausePoll = false, pollRunning = false;
   private java.text.NumberFormat nf = null;

   protected final ViewerTask pollMultiChannelTask = new PollMultiChannelTask();
   protected final ViewerTask pollSingleChannelTask = new PollSingleChannelTask();
   protected final ViewerTask setupViewerTask = new SetupViewerTask();

   /**
    * Constructs a new ADViewer, ready for display in a OneWireViewer tab.
    */
   public ADViewer()
   {
      // layout for Viewer
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 2;

      // element panel
      elementPanel = new JPanel();
      elementPanel.setLayout(new FlowLayout(FlowLayout.LEFT,10,10));
      JScrollPane elementScroll = new JScrollPane(elementPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      elementScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Channels"));

      // feature panel
      JPanel featurePanel = new JPanel(new GridLayout(TOTAL_FEATURES, 2, 3, 3));
      JScrollPane featureScroll = new JScrollPane(featurePanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      featureScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Features"));

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

      // add the panels
      add(featureScroll, BorderLayout.NORTH);
      add(elementScroll, BorderLayout.CENTER);

      clearContainer();
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
      if(owc instanceof ADContainer)
      {
         if(owc instanceof com.dalsemi.onewire.container.OneWireContainer41)
         {
            OneWireContainer41 l_owc41
               = (OneWireContainer41)owc;
            DSPortAdapter l_adapter = l_owc41.getAdapter();
            try
            {
               l_adapter.beginExclusive(true);
               byte configByte = l_owc41.getDeviceConfigByte();
               if(configByte != OneWireContainer41.DCB_DS2422)
               {
                  return false;
               }
            }
            catch ( Exception e )
            {}
            finally
            {
               l_adapter.endExclusive();
            }
         }
         return true;
      }
      return false;
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
            this.container = (ADContainer)owc;
            this.romID = owc.getAddressAsString();
            this.taggedDevice = null;
            this.pathToDevice = null;
            this.elements = null;
            this.includeAD = null;
         }
         enqueueRunTask(setupViewerTask);
      }
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
      //return (td instanceof A2D);
      //currently no support for a Tagged A2D device
      return false;
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      // currently not supported
      return;
      /*
      // ensure that the container was cleared previously
      if(this.adapter!=null || this.container!=null || this.romID!=null)
         clearContainer();

      if(td!=null)
      {
         synchronized(syncObj)
         {
            OneWireContainer owc = td.getDeviceContainer();
            this.adapter = owc.getAdapter();
            this.container = (ADContainer)owc;
            this.romID = owc.getAddressAsString();
            this.taggedDevice = td;
            this.pathToDevice = td.getOWPath();
            this.elements = null;
            this.includeAD = null;
         }
         enqueueRunTask(setupViewerTask);
      }
      */
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
         this.elements = null;
         this.includeAD = null;
      }
      setStatus(VERBOSE, "No Device");
      this.elementPanel.removeAll();
      for(int i=0; i<TOTAL_FEATURES; i++)
         lblFeature[i].setText("");
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
       // only return true if the task is not just thesetup task
       return (runList.size()>0)
          && !( (runList.size()==1) && (runList.indexOf(setupViewerTask)==0) );
    }

   /**
    * Run method services GUI interaction.  i.e., if the 'Increment'
    * button is pressed, it is this method which will service that
    * request and actually increment the wiper position.
    */
   public void run()
   {
      while(executeRunTask())
         /* no-op */;
   }

   /**
    * 'poll' method that is called at the current polling rate.
    * Read and update the display of the device status.
    */
   public void poll()
   {
      while(executePollTask())
         /* no-op */;

      synchronized(syncObj)
      {
         if(elements!=null)
         {
            boolean atLeastOne = false;
            for(int i=0; i<elements.length; i++)
            {
               includeAD[i] = elements[i].getIncludeAD();
               atLeastOne |= includeAD[i];
            }
            if(atLeastOne)
            {
               if(container!=null&&container.canADMultiChannelRead())
                  enqueuePollTask(pollMultiChannelTask);
               else
                  enqueuePollTask(pollSingleChannelTask);
            }
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
      ADViewer adv = new ADViewer();
      adv.setContainer((OneWireContainer)this.container);
      return adv;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * PollMultiChannelTask encapsulates the action of reading the multiple
    * channels of a 1-Wire AtoD Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class PollMultiChannelTask extends ViewerTask
   {

      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         ADContainer l_container = null;
         ADElement[] l_elements = null;
         boolean[] l_includeAD = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null || elements==null)
               return;
            l_adapter = adapter;
            l_container = container;
            l_elements = elements;
            l_includeAD = includeAD;
         }

         try
         {
            setStatus(VERBOSE, "Polling Multiple AtoD channels...");
            l_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = l_container.readDevice();
            if(l_container.canADMultiChannelRead())
            {
               l_container.doADConvert(l_includeAD, state);
               for(int i=0; i<l_elements.length; i++)
               {
                  double voltage = l_container.getADVoltage(i, state);
                  l_elements[i].setADVoltage(voltage);
                }
               setStatus(VERBOSE, "Done polling.");
            }
            else
               setStatus(VERBOSE, "Can't poll multiple channels.");
            if(pathToDevice!=null)
               pathToDevice.close();
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
    * PollTemperatureTask encapsulates the action of reading the temperature of
    * a 1-Wire Temperature Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class PollSingleChannelTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         ADContainer l_container = null;
         ADElement[] l_elements = null;
         boolean[] l_includeAD = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null || elements==null)
               return;
            l_adapter = adapter;
            l_container = container;
            l_elements = elements;
            l_includeAD = includeAD;
         }

         try
         {
            setStatus(VERBOSE, "Polling Single AtoD channels...");
            l_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = l_container.readDevice();
            for(int i=0; i<l_elements.length; i++)
            {
               if(l_includeAD[i])
               {
                  l_container.doADConvert(i, state);
                  double voltage = l_container.getADVoltage(i, state);
                  l_elements[i].setADVoltage(voltage);
               }
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
    * SetupViewerTask encapsulates the action of setting up the viewer for
    * a 1-Wire AtoD Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class SetupViewerTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         ADContainer l_container = null;
         ADElement[] l_elements = null;
         boolean[] l_includeAD = null;
         synchronized(syncObj)
         {
            l_adapter = ADViewer.this.adapter;
            l_container = ADViewer.this.container;
            if(l_adapter==null || l_container==null)
               return;
         }

         try
         {
            setStatus(MESSAGE, "Setting up viewer...");
            l_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = l_container.readDevice();
            int numChans = l_container.getNumberADChannels();
            l_elements = new ADElement[numChans];
            l_includeAD = new boolean[numChans];
            for(int i=0; i<numChans; i++)
            {
               l_includeAD[i] = true;
               l_elements[i] = new ADElement("Channel "+i);
               l_elements[i].setADRanges(l_container.getADRanges(i));
               l_elements[i].setADResolutions(
                                l_container.getADResolutions(i,
                                l_container.getADRange(i, state)) );
               l_elements[i].setADVoltage(
                                l_container.getADVoltage(i, state) );
               elementPanel.add(l_elements[i]);
            }
            lblFeature[NUM_CHANS].setText(" " + numChans);
            lblFeature[MULTI_CHAN].setText(" "
               + l_container.canADMultiChannelRead());
            lblFeature[HAS_ALARMS].setText(" " + l_container.hasADAlarms());

            synchronized(syncObj)
            {
               if(ADViewer.this.adapter==l_adapter
                  && ADViewer.this.container==l_container)
               {
                  elements = l_elements;
                  includeAD = l_includeAD;
               }
            }
            setStatus(MESSAGE, "Done Setting up viewer.");
         }
         catch(Exception e)
         {
            e.printStackTrace();
            setStatus(ERROR,"Error initializing viewer!" + e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }
}
