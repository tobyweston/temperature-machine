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
import javax.swing.text.*;
import java.awt.*;

import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.OneWireContainer41;
import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.utils.OWPath;

/**
 * A <code>OneWireContainer</code> viewer for integration in OneWireViewer.
 * Displays all basic information contained in <code>OneWireContainer</code>.
 *
 * @author SH
 * @version 1.0
 */
public class ContainerViewer
   extends Viewer
   implements Pollable
{
   private final String[] content = { "Device Address: ", null,
                                      "Name: ", null,
                                      "Alternate Names: ", null,
                                      "Description: ", null };
   private final String[] styles = { "bold", "regular" };

   /* visual components */
   private JTextPane textPane;
   private Document doc = null;

   private final JLabel[] taggedContentLabels = {
      new JLabel("Cluster: ", JLabel.RIGHT), new JLabel(),
      new JLabel("Device Label: ", JLabel.RIGHT), new JLabel(),
      new JLabel("Device Type: ", JLabel.RIGHT), new JLabel(),
      new JLabel("Path To Device: ", JLabel.RIGHT), new JLabel(),
      new JLabel("Response: ", JLabel.RIGHT), new JLabel("N/A")
   };

   private JPanel taggedContentPanel = null;

   private TaggedDevice taggedDevice = null;
   private OWPath pathToDevice = null;

   /**
    * Initializes the layout and sets up all visual components.
    */
   public ContainerViewer()
   {
      super("ContainerViewer");

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 2;

      this.textPane = new JTextPane();
      this.textPane.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "1-Wire Device Description"));
      this.textPane.setEditable(false);
      add(new JScrollPane(this.textPane,
                  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                  JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

      this.doc = this.textPane.getDocument();

      this.taggedContentPanel
         = new JPanel(new GridLayout(taggedContentLabels.length/2,2,3,3));
      this.taggedContentPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "1-Wire Tagging Info"));
      for(int i=0; i<taggedContentLabels.length; i+=2)
      {
         taggedContentLabels[i].setFont(Viewer.fontBold);
         taggedContentLabels[i].setForeground(Color.black);
         taggedContentLabels[i].setBackground(Color.lightGray);
         taggedContentLabels[i].setOpaque(true);
         taggedContentPanel.add(taggedContentLabels[i]);
         taggedContentLabels[i+1].setFont(fontPlain);
         taggedContentLabels[i+1].setForeground(Color.black);
         taggedContentLabels[i+1].setBackground(Color.lightGray);
         taggedContentLabels[i+1].setOpaque(true);
         taggedContentPanel.add(taggedContentLabels[i+1]);
      }

      initStyles();
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

      return true;
   }

   /**
    * Sets the container for this viewer.
    *
    * @param owc OneWireContainer of this viewer
    */
   public void setContainer(OneWireContainer owc)
   {
      if(owc!=null)
      {
         if(owc instanceof com.dalsemi.onewire.container.OneWireContainer41)
         {
             OneWireContainer41 l_owc41
               = (OneWireContainer41)owc;
            DSPortAdapter l_adapter = l_owc41.getAdapter();
            try
            {
               l_adapter.beginExclusive(true);
               // read the device config byte just to force the init of 
               // description and other parameters
               byte configByte = l_owc41.getDeviceConfigByte();
            }
            catch ( Exception e )
            {}
            finally
            {
               l_adapter.endExclusive();
            }
         }
         
         this.adapter = owc.getAdapter();

         content[1] = owc.getAddressAsString() + " (" + 
                      com.dalsemi.onewire.utils.Convert.toHexString(
                         owc.getAddress(), " ") + 
                      ")\r\n\r\n";
         content[3] = owc.getName() + "\r\n\r\n";
         content[5] = owc.getAlternateNames() + "\r\n\r\n";
         content[7] = owc.getDescription() + "\r\n\r\n";

         clearContent();
         appendContent("\r\n", styles[1]);
         for(int i=0; i<content.length; i++)
            appendContent(content[i], styles[i%2]);
      }
      else
      {
         clearContainer();
      }
   }

   private void clearContent()
   {
      try
      {
         this.taggedDevice = null;
         this.pathToDevice = null;
         doc.remove(0, doc.getLength());
         remove(taggedContentPanel);
      }
      catch (BadLocationException ble)
      {
         ble.printStackTrace();
      }
   }

   private void appendContent(String content, String style)
   {
      try
      {
         doc.insertString(doc.getLength(), content, textPane.getStyle(style));
      }
      catch (BadLocationException ble)
      {
         ble.printStackTrace();
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
      return true;
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      if(td!=null)
      {
         synchronized(syncObj)
         {
            setContainer(td.getDeviceContainer());

            this.taggedDevice = td;
            this.pathToDevice = td.getOWPath();

            this.taggedContentLabels[1].setText(td.getClusterName());
            this.taggedContentLabels[3].setText(td.getLabel());
            this.taggedContentLabels[5].setText(td.getDeviceType());
            this.taggedContentLabels[7].setText(this.pathToDevice+"");
            this.taggedContentLabels[9].setText("N/A");

            add(taggedContentPanel, BorderLayout.NORTH);
         }
      }
      else
      {
         clearContainer();
      }
   }

   public void poll()
   {
      OWPath l_path = null;
      TaggedDevice l_td = null;
      DSPortAdapter l_adapter = null;
      synchronized(syncObj)
      {
         if(this.taggedDevice==null)
            return;
         l_td = this.taggedDevice;
         l_adapter = this.adapter;
         l_path = this.pathToDevice;
      }

      // should be a super-class or an interface for readSensor method
      // then wouldn't need so many 'if-else' blocks, just one.
      if(l_td instanceof Contact)
      {
         try
         {
            l_adapter.beginExclusive(true);
            if(l_path!=null)
               l_path.open();
            this.taggedContentLabels[9].setText(((Contact)l_td).readSensor());
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Error occured during polling: " + e);
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
      else if(l_td instanceof Humidity)
      {
         try
         {
            l_adapter.beginExclusive(true);
            if(l_path!=null)
               l_path.open();
            this.taggedContentLabels[9].setText(((Humidity)l_td).readSensor());
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Error occured during polling: " + e);
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
      else if(l_td instanceof Thermal)
      {
         try
         {
            l_adapter.beginExclusive(true);
            if(l_path!=null)
               l_path.open();
            this.taggedContentLabels[9].setText(((Thermal)l_td).readSensor());
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Error occured during polling: " + e);
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
      else
      {
         this.taggedContentLabels[9].setText("N/A");
      }
   }

   /**
    * Clears the reference to the device container.
    */
   public void clearContainer()
   {
      Document doc = textPane.getDocument();
      try
      {
         doc.remove(0,doc.getLength());
         doc.insertString(0, "\r\nNo Device Selected!",
                          textPane.getStyle("bold"));
      }
      catch (BadLocationException ble)
      {
         System.err.println("Couldn't insert initial text.");
      }
   }

   /**
    * Gets the string that represents this Viewer's title
    *
    * @return viewer's title
    */
   public String getViewerTitle()
   {
      return "Description";
   }

   /**
    * Gets the string that represents this Viewer's description
    *
    * @return viewer's description
    */
   public String getViewerDescription()
   {
      return "Detailed Description of 1-Wire Devices";
   }

   /**
    * Returns <code>true</code> if Viewer still has pending tasks it must
    * complete.
    *
    * @return <code>false</code>, always since this viewer is never busy.
    */
    public boolean isBusy()
    {
       return false;
    }

   private void initStyles()
   {
      Style def = StyleContext.getDefaultStyleContext().
                               getStyle(StyleContext.DEFAULT_STYLE);

      Style regular = textPane.addStyle("regular", def);
      StyleConstants.setFontFamily(def, "Courier New");
      StyleConstants.setFontSize(def, 14);

      Style s = textPane.addStyle("italic", regular);
      StyleConstants.setItalic(s, true);

      s = textPane.addStyle("bold", regular);
      StyleConstants.setBold(s, true);

      s = textPane.addStyle("small", regular);
      StyleConstants.setFontSize(s, 10);

      s = textPane.addStyle("large", regular);
      StyleConstants.setFontSize(s, 18);
   }

   /**
    * Create a complete clone of this viewer, including reference to
    * current container.  Used to display viewer in new window.
    */
   public Viewer cloneViewer()
   {
      ContainerViewer cv = new ContainerViewer();
      if(this.taggedDevice!=null)
         cv.setContainer(this.taggedDevice);
      else
      {
         Document doc = cv.textPane.getDocument();
         try
         {
            doc.remove(0,doc.getLength());
            for (int i=0; i < content.length; i++)
            {
               doc.insertString(doc.getLength(), content[i],
                                textPane.getStyle(styles[i%2]));
            }
         }
         catch (BadLocationException ble)
         {
            System.err.println("Couldn't insert initial text.");
         }
      }
      return cv;
   }
}
