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
import com.dalsemi.onewire.container.PotentiometerContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.application.tag.D2A;
import com.dalsemi.onewire.utils.OWPath;

/**
 * A <code>PotentiometerContainer</code> Viewer for integration in the
 * OneWireViewer. All devices that implement <code>PotentiometerContainer</code>
 * are supported by this viewer.  This viewer creates a list of potentiometer
 * elements (actual number of wipers which the device supports) and allows the
 * user to see the current position of the wiper (and it's value of resistance)
 * as well as update the position of the wiper.
 *
 * @author SH
 * @version 1.00
 */
public class PotentiometerViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "PotentiometerViewer";
   private static final String strTab = "Potentiometer";
   private static final String strTip = "Shows Variable Potentiometer Status";

   /* container variables */
   private PotentiometerContainer container  = null;
   private D2A taggedDevice = null;
   private OWPath pathToDevice = null;

   /* visual components */
   private JPanel elementPanel = null;
   private PotentiometerElement[] elements = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = { "Potentiometer Resistance ",
                                  "Number of Potentiometers ",
                                  "Number of Wiper Settings ",
                                  "Are Wipers Volatile? ",
                                  "Has linear elements? " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 5;
   private static final int RESISTANCE = 0;
   private static final int NUM_POTS = 1;
   private static final int NUM_WIPER_SETTINGS = 2;
   private static final int WIPERS_VOLATILE = 3;
   private static final int IS_LINEAR = 4;

   /* single-instance Viewer Tasks */
   private ViewerTask setupViewerTask = new SetupViewerTask();
   private ViewerTask setWiperPosition = new SetWiperPosition();
   private ViewerTask pollWiperPosition = new PollWiperPosition();

   /**
    * Creates a new PotentiometerViewer for integration in the OneWireViewer.
    */
   public PotentiometerViewer()
   {
      // layout for Viewer
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 3;

      // element panel
      elementPanel = new JPanel();
      elementPanel.setLayout(new FlowLayout(FlowLayout.LEFT,10,10));
      JScrollPane elementScroll = new JScrollPane(elementPanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      elementScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Wipers"));

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
         lblFeatureHdr[i].setFont(fontPlain);
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
      return (owc instanceof PotentiometerContainer);
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
            this.container = (PotentiometerContainer)owc;
            this.romID = owc.getAddressAsString();
            this.elements = null;
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
      return (td instanceof D2A);
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
            this.container = (PotentiometerContainer)owc;
            this.romID = owc.getAddressAsString();
            this.elements = null;
            this.taggedDevice = (D2A)td;
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
         this.elements = null;
         this.pathToDevice = null;
         this.taggedDevice = null;
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
       //return (runList.size()>0)
       //   && !( (runList.size()==1) && (runList.indexOf(setupViewerTask)==0) );
       
       // just return false.  potentiometer update tasks can be safely ignored
       return false;
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

      enqueueRunTask(setWiperPosition);
   }

   /**
    * 'poll' method that is called at the current polling rate.
    * Read and update the display of the device status.
    */
   public void poll()
   {
      while(executePollTask())
         /* no-op */;
      enqueuePollTask(pollWiperPosition);
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
      PotentiometerViewer pv = new PotentiometerViewer();
      pv.setContainer((OneWireContainer)this.container);
      return pv;
   }

   /**
    * SetupViewerTask encapsulates the action of setting up the viewer for
    * a 1-Wire Potentiometer Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class SetupViewerTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         PotentiometerContainer l_container = null;
         PotentiometerElement[] l_elements = null;
         synchronized(syncObj)
         {
            l_adapter = PotentiometerViewer.this.adapter;
            l_container = PotentiometerViewer.this.container;
         }

         if(l_adapter==null || l_container==null)
            return;

         try
         {
            setStatus(MESSAGE, "Setting up viewer.");
            l_adapter.beginExclusive(true);
            byte[] state = l_container.readDevice();
            int numPots = l_container.numberOfPotentiometers(state);
            int numWipes = l_container.numberOfWiperSettings(state);
            double maxRes = l_container.potentiometerResistance(state);
            l_elements = new PotentiometerElement[numPots];
            for(int i=0; i<numPots; i++)
            {
               l_elements[i] = new PotentiometerElement("Wiper "+i);
               l_container.setCurrentWiperNumber(i, state);
               int pos = l_container.getWiperPosition();
               double res = (maxRes*pos/numWipes);
               l_elements[i].setResistance(pos, numWipes, maxRes);
               l_elements[i].setChargePump(
                  l_container.isChargePumpOn(state));

               //only show appropriate channels for tagged devices
               if(taggedDevice==null || taggedDevice.getChannel()==i)
                  elementPanel.add(l_elements[i]);
            }
            lblFeature[RESISTANCE].setText(" " + maxRes + " kOhms");
            lblFeature[NUM_POTS].setText(" "+numPots);
            lblFeature[NUM_WIPER_SETTINGS].setText(" "+numWipes);
            lblFeature[WIPERS_VOLATILE].setText(
               " " + l_container.wiperSettingsAreVolatile(state));
            lblFeature[IS_LINEAR].setText(" "+l_container.isLinear(state));

            synchronized(syncObj)
            {
               if(PotentiometerViewer.this.adapter==l_adapter
                  && PotentiometerViewer.this.container==l_container)
               {
                  elements = l_elements;
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

   protected class SetWiperPosition extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         PotentiometerContainer l_container = null;
         PotentiometerElement[] l_elements = null;
         synchronized(syncObj)
         {
            l_adapter = PotentiometerViewer.this.adapter;
            l_container = PotentiometerViewer.this.container;
            l_elements = PotentiometerViewer.this.elements;
         }

         if(l_adapter==null || l_container==null || l_elements==null)
            return;

         boolean deviceUpdate = false;
         // check all elements to see if an action was executed
         for(int i=0; i<l_elements.length; i++)
         {
            if(l_elements[i].hasSliderMoved())
            {
               try
               {
                  l_adapter.beginExclusive(true);
                  byte[] state = l_container.readDevice();
                  l_container.setCurrentWiperNumber(i,state);
                  l_container.setWiperPosition(l_elements[i].getSliderPosition());
                  deviceUpdate = true;
               }
               catch(Exception e)
               {
                  setStatus(ERROR, "Error updating wiper! "+e.toString());
               }
               finally
               {
                  l_adapter.endExclusive();
               }
            }// end if slider moved

            if(l_elements[i].hasToggleButtonClick())
            {
               try
               {
                  l_adapter.beginExclusive(true);
                  byte[] state = l_container.readDevice();
                  boolean val = l_container.isChargePumpOn(state);
                  l_container.setChargePump(!val, state);
                  l_container.writeDevice(state);
                  state = l_container.readDevice();
                  if(val==l_container.isChargePumpOn(state))
                  {
                     setStatus(WARNMSG,
                      "Failed to toggle charge pump, must have external power");
                  }
                  deviceUpdate = true;
               }
               catch(Exception e)
               {
                  setStatus(ERROR, "Error Toggling Charge Pump! "+e.toString());
               }
               finally
               {
                  l_adapter.endExclusive();
               }
            }// end if toggle button clicked

            l_elements[i].clearButtonClick();
         }//end for loop

         if(deviceUpdate)
            poll();
      }
   }

   protected class PollWiperPosition extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         PotentiometerContainer l_container = null;
         PotentiometerElement[] l_elements = null;
         synchronized(syncObj)
         {
            l_adapter = PotentiometerViewer.this.adapter;
            l_container = PotentiometerViewer.this.container;
            l_elements = PotentiometerViewer.this.elements;
         }

         if(l_adapter==null || l_container==null || l_elements==null)
            return;

         for(int i=0; i<l_elements.length; i++)
         {
            try
            {
               setStatus(VERBOSE, "Polling potentiometer wiper...");
               l_adapter.beginExclusive(true);
               byte[] state = l_container.readDevice();
               l_container.setCurrentWiperNumber(i, state);
               int pos = l_container.getWiperPosition();
               double maxRes = l_container.potentiometerResistance(state);
               int wipes = l_container.numberOfWiperSettings(state);
               l_elements[i].setResistance(pos, wipes, maxRes);
               l_elements[i].setChargePump(l_container.isChargePumpOn(state));
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
   }
}
