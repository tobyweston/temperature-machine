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
import java.util.Date;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.HumidityContainer;
import com.dalsemi.onewire.container.OneWireContainer41;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.application.tag.Humidity;
import com.dalsemi.onewire.utils.OWPath;

/**
 * A <code>HumidityContainer</code> Viewer for integration in the OneWireViewer.
 * All devices that implement <code>HumidityContainer</code> are supported by
 * this viewer.  This viewer displays (and plots over time) the value of
 * the relative humidity reported by the device.
 *
 * @author SH
 * @version 1.00
 */
public class HumidityViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "HumidityViewer";
   private static final String strTab = "Humidity";
   private static final String strTip = "Shows relative humidity";
   private static final String naString = "N/A";

   /* container variables */
   private HumidityContainer container  = null;
   private Humidity taggedDevice = null;
   private OWPath pathToDevice = null;

   /* visual components */
   private Plot plot = null;
   private AdjustableMeter meter = null;
   private JComboBox resolutionList = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = { "Humidity " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 1;
   private static final int HUMD=0;

   private volatile boolean pausePoll = false, pollRunning = false;
   private static final java.text.NumberFormat nf = 
      new java.text.DecimalFormat();
   private static final java.text.DateFormat df = 
      java.text.DateFormat.getDateTimeInstance(
         java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM);

   /* Single-instance Viewer Tasks */
   private final ViewerTask pollHumidityTask = new PollHumidityTask();
   private final ViewerTask pollResolutionTask = new PollResolutionTask();
   private final ViewerTask setupViewerTask = new SetupViewerTask();

   /**
    * Constructs a new HumidityViewer.
    */
   public HumidityViewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 3;

      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);

      // meter panel
      meter = new AdjustableMeter(0,100);
      JPanel meterPanel = new JPanel(new BorderLayout());
      meterPanel.add(meter, BorderLayout.CENTER);
      JScrollPane meterScroll = new JScrollPane(meterPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      meterScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Humidity"));

      // feature panel
      JPanel featurePanel = new JPanel(new GridLayout(2, 2, 3, 3));
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
         lblFeatureHdr[i].setFont(fontPlain);
         lblFeature[i].setForeground(Color.black);
         lblFeature[i].setBackground(Color.lightGray);

         featurePanel.add(lblFeatureHdr[i]);
         featurePanel.add(lblFeature[i]);
      }

      JLabel lblRes = new JLabel("Resolution ", JLabel.RIGHT);
      lblRes.setFont(fontBold);
      lblRes.setOpaque(true);
      lblRes.setBackground(Color.lightGray);
      lblRes.setForeground(Color.black);
      featurePanel.add(lblRes);

      JPanel resPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
      resPanel.setBackground(Color.lightGray);
      resolutionList = new JComboBox();
      resolutionList.setFont(fontPlain);
      resolutionList.addItem(naString);
      resolutionList.setPreferredSize(new Dimension(49,26));
      resolutionList.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent e)
            {
               synchronized(syncObj)
               {
                  if(!container.hasSelectableHumidityResolution())
                     return;
                  if(pausePoll)
                     return;
                  Object o = resolutionList.getSelectedItem();
                  if(o!=null && o!=naString)
                  {
                     try
                     {
                        double d = Double.parseDouble(o.toString());
                        enqueueRunTask(new ChangeResolutionTask(adapter,
                                            container, d));
                     }
                     catch(NumberFormatException nfe)
                     {;}
                  }
               }
            }
         }
      );
      resPanel.add(resolutionList);
      featurePanel.add(resPanel);

      // graph
      JPanel graphPanel = new JPanel(new GridLayout(1, 1, 3, 3));
      JScrollPane graphScroll = new JScrollPane(graphPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      graphScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Graph"));
      plot = new Plot();
      plot.setScale(0, 100);
      graphPanel.add(plot);

      add(featureScroll, BorderLayout.NORTH);
      add(graphScroll, BorderLayout.CENTER);
      add(meterScroll, BorderLayout.EAST);

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
      if(owc instanceof HumidityContainer)
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
               if(configByte != OneWireContainer41.DCB_DS1923)
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
            this.container = (HumidityContainer)owc;
            this.romID = owc.getAddressAsString();
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
      if(td instanceof Humidity)
      {
         return true;
      }
      return false;
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      // ensure that the container was cleared previously
      if(this.adapter!=null || this.container!=null || this.romID!=null)
         clearContainer();

      if(td!=null)
      {
         synchronized(syncObj)
         {
            OneWireContainer owc = td.getDeviceContainer();
            this.adapter = owc.getAdapter();
            this.container = (HumidityContainer)owc;
            this.romID = owc.getAddressAsString();
            this.taggedDevice = (Humidity)td;
            this.pathToDevice = td.getOWPath();
         }
         enqueueRunTask(setupViewerTask);
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
         this.taggedDevice = null;
         this.pathToDevice = null;
      }
      setStatus(VERBOSE, "No Device");
      meter.resetValue();
      plot.resetPlot();
      for(int i=0; i<TOTAL_FEATURES; i++)
         lblFeature[i].setText("");
   }

   protected void pausePoll()
   {
      for(boolean paused = false; !paused;)
      {
         synchronized(syncObj)
         {
            this.pausePoll = true;
            paused = !this.pollRunning;
         }
      }
   }

   protected void resumePoll()
   {
      for(boolean resumed = false; !resumed;)
      {
         synchronized(syncObj)
         {
            this.pausePoll = false;
            resumed = this.pollRunning;
         }
      }
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
    * 'run' method that is called continuously to service
    * GUI interaction, such as button click events.  Also
    * performs one-time setup of viewer.
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
      synchronized(syncObj)
      {
         if(this.pausePoll)
            return;
         this.pollRunning = true;
      }

      while(executePollTask())
         /* no-op */;

      enqueuePollTask(pollHumidityTask);
      enqueuePollTask(pollResolutionTask);

      synchronized(syncObj)
      {
         pollRunning = false;
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
      HumidityViewer hv = new HumidityViewer();
      hv.setContainer((OneWireContainer)this.container);
      return hv;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * PollTemperatureTask encapsulates the action of reading the temperature of
    * a 1-Wire Temperature Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class PollHumidityTask extends ViewerTask
   {
      double lastHumidityRead = Double.NaN;
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         HumidityContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         setStatus(VERBOSE, "Polling Humidity...");
         try
         {
            l_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = l_container.readDevice();
            l_container.doHumidityConvert(state);
            double read = l_container.getHumidity(state);
            if(read!=this.lastHumidityRead)
            {
               this.lastHumidityRead = read;
               lblFeature[HUMD].setText(" "+nf.format(read)+" %");
               meter.setValue(read);
            }
            plot.addPoint(read, 
               df.format(new Date()) + ",%RH");
            setStatus(VERBOSE, "Done polling.");
         }
         catch(Exception e)
         {
            this.lastHumidityRead = Double.NaN;
            setStatus(ERROR, "Error reading device! "+e.toString());
            meter.resetValue();
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * PollResolutionTask encapsulates the action of reading the resolution of
    * a 1-Wire Temperature Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class PollResolutionTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         HumidityContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         if(!l_container.hasSelectableHumidityResolution())
            return;

         setStatus(VERBOSE, "Polling Resolution...");
         try
         {
            l_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = l_container.readDevice();
            String res = ""+l_container.getHumidityResolution(state);
            if(!res.equals(resolutionList.getSelectedItem().toString()))
            {
               resolutionList.setSelectedItem(res);
            }
            setStatus(VERBOSE, "Done polling.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error reading device! "+e.toString());
            meter.resetValue();
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * PollResolutionTask encapsulates the action of reading the resolution of
    * a 1-Wire Temperature Device.
    */
   protected class ChangeResolutionTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      HumidityContainer task_container = null;
      double task_newResolution = Double.NaN;

      public ChangeResolutionTask(DSPortAdapter l_adapter,
                            HumidityContainer l_container,
                            double newResolution)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_newResolution = newResolution;
      }

      public void executeTask()
      {
         pausePoll();
         setStatus(VERBOSE, "Changing resolution");
         try
         {
            task_adapter.beginExclusive(true);
            if(pathToDevice!=null)
               pathToDevice.open();
            byte[] state = task_container.readDevice();
            if(task_container.getHumidityResolution(state)!=task_newResolution)
            {
               task_container.setHumidityResolution(task_newResolution, state);
               task_container.writeDevice(state);
               String res = String.valueOf(task_newResolution);
               if(!res.equals(resolutionList.getSelectedItem().toString()))
               {
                  resolutionList.setSelectedItem(res);
               }
               setStatus(VERBOSE, "Done changing resolution");
            }
            else
               setStatus(VERBOSE, "No need to change resolution");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Resolution Change Error: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
         resumePoll();
      }
   }

   /**
    * SetupViewerTask encapsulates the action of setting up the viewer for
    * a 1-Wire Temperature Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class SetupViewerTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         HumidityContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         pausePoll();
         setStatus(VERBOSE, "Setting up viewer");
         resolutionList.removeAllItems();
         if(l_container.hasSelectableHumidityResolution())
         {
            double[] resolutions =
               l_container.getHumidityResolutions();
            for(int i=0; i<resolutions.length; i++)
            {
               resolutionList.addItem(""+resolutions[i]);
            }
            resolutionList.setEnabled(true);
            pollResolutionTask.executeTask();
         }
         else
         {
            resolutionList.addItem(naString);
            resolutionList.setEnabled(false);
         }
         pollHumidityTask.executeTask();
         setStatus(VERBOSE, "Done Setting up viewer");
         resumePoll();
      }
   }
}
