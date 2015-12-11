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

import javax.swing.border.TitledBorder;
import javax.swing.*;
import java.awt.*;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.SwitchContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.*;

/**
 * A <code>SwitchContainer</code> Viewer for integration in the OneWireViewer.
 * All devices that implement <code>SwitchContainer</code> are supported by
 * this viewer.  This viewer creates a list of switch elements (actual number
 * of switches which the device supports) and allow the user to see the current
 * state of activity on each 1-Wire switch device as well as toggle their state.
 *
 * @author DS
 * @version 1.10
 */
public class SwitchViewer
   extends Viewer
   implements Runnable, Pollable
{

   //--------
   //-------- Finals
   //--------

   /** Viewer title */
   private static final String strTitle = "SwitchViewer";

   /** Viewer Tab */
   private static final String strTab = "Switch";

   /** Viewer Tip */
   private static final String strTip = "Shows Device Latch Status";

   //--------
   //-------- Variables
   //--------

   /** Field container - local reference to this device */
   private SwitchContainer container = null;

   /** Field taggedDevice - local reference to this tagged-device */
   private TaggedDevice taggedDevice = null;

   /** Field elements - array of switch element, one per channel */
   private SwitchElement[] elements;

   /** Field didSetup - flag to indicate the initial read was performed */
   private boolean didSetup = false;

   /** Field elementPanel - panel for channel elements */
   private JPanel elementPanel;

   /** Field elementScroll - scroll panel for channel elements */
   private JScrollPane elementScroll;

   /** Field featureScroll scroll panel for feature list */
   private JScrollPane featureScroll;

   /** Field featurePanel - panel for feature list */
   private JPanel featurePanel;

   /** Field lblFeature[] - array of feature labels */
   private JLabel[] lblFeature = null;
   /** Field lblFeatureHdr[] - array of headers for feature labels */
   private JLabel[] lblFeatureHdr = null;
   /** Field lblFeatureHdr[] - array of strings for header content */
   private String[] strHeader = { "Activity Sensing ",
                                  "Level sensing ",
                                  "'Smart-on' ",
                                  "High-side switch (on connected to data) ",
                                  "One switch on at a time limit " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 5;
   private static final int ACTIVITY = 0;
   private static final int LEVEL = 1;
   private static final int SMARTON = 2;
   private static final int HIGHSIDE = 3;
   private static final int ONELIMIT = 4;

   /** Field hasState - flag to indicate state setting is supported
    *  by this type
    */
   private boolean hasState = true;

   /** Field hasActivity - flag to indicate activity sensing is supported
    *  by this type
    */
   private boolean hasActivity = false;

   /** Field hasLevel- flag to indicate level sensing is supported
    *  by this type
    */
   private boolean hasLevel = false;

   //--------
   //-------- Constructors
   //--------

   /**
    * Constructor for a SwitchViewer viewer.  Create all of the visual
    * objects that do not depend on the features of switch (like number
    * of channels.
    */
   public SwitchViewer()
   {
      // layout for Viewer
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 1;

      // element panel
      elementPanel = new JPanel();
      elementPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));
      elementScroll =
         new JScrollPane(elementPanel,
                         JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                         JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      elementScroll.setBorder(
         BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Channels"));

      // feature list (start blank)
      featurePanel = new JPanel();
      featurePanel.setLayout(new GridLayout(5, 2, 3, 3));
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
         lblFeature[i].setFont(fontPlain);
         lblFeature[i].setOpaque(true);
         lblFeature[i].setForeground(Color.black);
         lblFeature[i].setBackground(Color.lightGray);

         featurePanel.add(lblFeatureHdr[i]);
         featurePanel.add(lblFeature[i]);
      }

      featureScroll = new JScrollPane(featurePanel);
      featureScroll.setBorder(
         BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Features"));

      // add components to the Viewer
      featureScroll.setVisible(false);
      add(featureScroll, BorderLayout.NORTH);
      add(elementScroll, BorderLayout.CENTER);

      // initialization
      clearContainer();
   }

   //--------
   //-------- Methods
   //--------

   /**
    * Clears the reference to the device container.
    */
   public void clearContainer()
   {
      synchronized (syncObj)
      {
         this.container = null;
         this.adapter   = null;
         this.romID     = "(NO DEVICE)";
      }
      elementPanel.removeAll();
      elementPanel.repaint();
      for(int i=0; i<TOTAL_FEATURES; i++)
         lblFeature[i].setText("");
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      this.taggedDevice = td;

      // ensure that the container was cleared previously
      if(this.adapter!=null || this.container!=null || this.romID!=null)
         clearContainer();

      if (td != null)
      {
         // reset the element state
         synchronized (syncObj)
         {
            OneWireContainer owc = td.getDeviceContainer();
            this.container = (SwitchContainer) owc;
            if (owc != null)
            {
               this.romID     = owc.getAddressAsString();
               this.adapter   = owc.getAdapter();
            }
   
            elements = null;
            didSetup = false;
         }
      }
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

      if (owc != null)
      {
         // reset the element state
         synchronized (syncObj)
         {
            this.taggedDevice = null;
            this.container = (SwitchContainer) owc;
            this.romID     = owc.getAddressAsString();
            this.adapter   = owc.getAdapter();

            elements = null;
            didSetup = false;
         }
      }
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
      return (owc instanceof SwitchContainer);
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
      if (td != null)
         return (td.getDeviceContainer() instanceof SwitchContainer);
      else
         return false;
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
    * Returns <code>true</code> if Viewer still has pending tasks it must
    * complete.
    *
    * @return <code>true</code> if Viewer still has pending tasks it must
    * complete.
    */
    public boolean isBusy()
    {
       return hasRunTasks();
    }

   /**
    * 'run' method that is called continuously to service
    * GUI interaction, such as button click events.  Also
    * performs one-time setup of viewer.
    */
   public void run()
   {
      boolean got_click = false;
      byte[]  state     = null;

      setStatus(VERBOSE, "Check button events...");

      // protect update from change in container
      synchronized (syncObj)
      {
         try
         {
            // check for no container yet
            if (container == null)
            {
               setStatus(WARNMSG, "1-Wire Container has not been set!");

               return;
            }

            // check for first time
            if (!didSetup)
            {
               setStatus(MESSAGE, "Setting up the Viewer");

               // get use of 1-Wire and read the state of the switch
               adapter.beginExclusive(true);

               state = container.readDevice();

               // create the feature list
               hasActivity = container.hasActivitySensing();
               hasLevel    = container.hasLevelSensing();
               hasState    = true;

               lblFeature [ACTIVITY].setText(" " + String.valueOf(hasActivity));
               lblFeature [LEVEL].setText(" " + String.valueOf(hasLevel));
               lblFeature [SMARTON].setText(
                  " " + String.valueOf(container.hasSmartOn()));
               lblFeature [HIGHSIDE].setText(
                  " " + String.valueOf(container.isHighSideSwitch()));
               lblFeature [ONELIMIT].setText(
                  " " + String.valueOf(container.onlySingleChannelOn()));

               // hide features if doing tags
               featureScroll.setVisible(taggedDevice == null);
            
               if (taggedDevice != null)
                  ((TitledBorder)elementScroll.getBorder()).setTitle("");
               else
                  ((TitledBorder)elementScroll.getBorder()).setTitle("Channels");
                
               // create elements
               elements =
                  new SwitchElement [container.getNumberChannels(state)];

               for (int ch = 0; ch < elements.length; ch++)
               {
                  String channel_title = "CHANNEL " + ch;

                  // if tagging, customise visible fields
                  if (taggedDevice != null)
                  {
                     if (taggedDevice.getLabel() != null)
                       channel_title = taggedDevice.getLabel();

                     if (taggedDevice instanceof Switch)
                     {
                        hasActivity = false;
                        hasLevel = false;
                     }

                     if (taggedDevice instanceof com.dalsemi.onewire.application.tag.Event)
                     {
                        hasState = false;
                        hasLevel = false;
                     }

                     if (taggedDevice instanceof Level)
                     {
                        hasState = false;
                        hasActivity = false;
                     }
                  }                     

                  // create the element
                  elements [ch] = new SwitchElement(channel_title, hasState, hasActivity,
                                                    hasLevel);

                  // Optionally make some channels not visible if doing tagging
                  if (taggedDevice != null)
                  {
                     if (taggedDevice.getChannel() != ch)
                        elements[ch].setVisible(false);
                  }
                  
                  // add element to the element panel
                  elementPanel.add(elements [ch]);
               }

               didSetup = true;

               setStatus(MESSAGE, "Setup complete");
            }
         }
         catch (Exception e)
         {
            setStatus(ERRMSG, "Error setting up device, " + e.toString());
         }
         finally
         {
            adapter.endExclusive();
         }

         // check for button clicks
         SwitchElement[] local_elements = elements;
         if (local_elements != null)
         {
            // check for click events
            for (int ch = 0; ch < local_elements.length; ch++)
            {
               // ask element if there has been button click activity
               if ((local_elements [ch].hasClearButtonClick())
                       || (local_elements [ch].hasToggleButtonClick()))
                  got_click = true;
            }
         }
      }

      setStatus(VERBOSE, "Check complete");

      // if there was a click event then
      if (got_click)
      {
         setStatus(MESSAGE, "Button event found");
         poll();
      }
   }

   /**
    * 'poll' method that is called at the current polling rate.
    * Read and update the display of the device status.
    */
   public void poll()
   {
      DSPortAdapter local_adapter = null;
      SwitchContainer local_container = null;
      SwitchElement[] local_elements = null;
      byte[] state = null;
      boolean value = false; 
      String value_string = null;

      setStatus(VERBOSE, "Poll reading...");

      // protect update from change in container
      synchronized (syncObj)
      {
         local_adapter = this.adapter;
         local_container = this.container;
         local_elements = elements;
      }

      try
      {
         // check for no container yet
         if ((local_container == null) || (!didSetup) || (local_elements == null))
         {
            setStatus(WARNMSG, "1-Wire Container has not been set!");
            return;
         }

         // get use of 1-Wire and read the state of the switch
         local_adapter.beginExclusive(true);
         state = local_container.readDevice();

         // loop while need to doing a write
         boolean do_write = false;
         do
         {
            // check if need to do the write (from previous button click)
            if (do_write)
            {
               setStatus(MESSAGE,"Writing state of switch");
               local_container.writeDevice(state);
               state = local_container.readDevice();
               do_write = false;
            }

            // get state of each element
            for (int ch = 0;  ch < local_elements.length; ch++)
            {
               if (hasState)
               {
                  value = local_container.getLatchState(ch,state);

                  value_string = null;
                  if (taggedDevice != null)
                  {
                     if (value)
                        value_string = taggedDevice.getMax();
                     else                                          
                        value_string = taggedDevice.getMin();
                  }

                  if (value_string == null)
                     value_string = String.valueOf(value);

                  local_elements[ch].setState(value_string);
               }

               if (hasLevel)
               {
                  value = local_container.getLevel(ch,state);

                  value_string = null;
                  if (taggedDevice != null)
                  {
                     if (value)
                        value_string = taggedDevice.getMax();
                     else                                          
                        value_string = taggedDevice.getMin();
                  }

                  if (value_string == null)
                     value_string = String.valueOf(value);

                  local_elements[ch].setLevel(value_string);
               }

               if (hasActivity)
               {
                  value = local_container.getSensedActivity(ch,state);

                  value_string = null;
                  if (taggedDevice != null)
                  {
                     if (value)
                        value_string = taggedDevice.getMax();
                     else                                          
                        value_string = "";
                  }

                  if (value_string == null)
                     value_string = String.valueOf(value);

                  local_elements[ch].setActivity(value_string);
               }

               // ask element if there has been button click activity
               if (local_elements[ch].hasClearButtonClick())
               {
                  setStatus(MESSAGE,"Clear Button event found on channel " + ch);
                  local_container.clearActivity();
                  do_write = true;
               }

               if (local_elements[ch].hasToggleButtonClick())
               {
                  setStatus(MESSAGE,"Toggle Button event found on channel " + ch);
                  local_container.setLatchState(ch, !local_container.getLatchState(ch,state), false, state);
                  do_write = true;
               }

               local_elements[ch].buttonClickClear();
            }
         }
         while (do_write);
      }
      catch (Exception e)
      {
         setStatus(ERRMSG, "Error polling device, " + e.toString());
      }
      finally
      {
         local_adapter.endExclusive();
      }

      setStatus(VERBOSE, "Poll complete");
   }

   /**
    * Create a complete clone of this viewer, including reference to
    * current container.  Used to display viewer in new window.
    */
   public Viewer cloneViewer()
   {
      SwitchViewer sv = new SwitchViewer();
      if (this.taggedDevice != null)
         sv.setContainer(this.taggedDevice);
      else
         sv.setContainer((OneWireContainer)this.container);
      return sv;
   }
}
