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
import javax.swing.event.*;
import java.awt.event.*;
import java.awt.*;

import com.dalsemi.onewire.container.OneWireContainer33;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.utils.Convert;

/**
 * A DS1963S/DS2432 (family code 0x33) viewer for integration in the OneWireViewer.
 * This viewer allows the user to manipulate the memory of the DS1961S iButton
 * as well as exercise all of the SHA specific features of the part.
 *
 * @author SH
 * @version 1.00
 */
public class SHA33Viewer extends Viewer
   implements Runnable
{
   /* string constants */
   private static final String strTitle = "SHA33Viewer";
   private static final String strTab = "DS1961S SHA";
   private static final String strTabAlt = "DS2432 SHA";
   private static final String strTip = "Exercises SHA functionality of DS1961S";
   private static final String naString = "N/A";

   /* container variables */
   private OneWireContainer33 container  = null;
   private TaggedDevice taggedDevice = null;

   /* visual components */
   private JTextField txtTargetPage = null, txtTargetOffset = null;
   private JTextField txtMac, txtSecret;
   private JTextArea txtScratchpad = null, txtDataPage = null;
   private JList pageList = null;
   private String[] pageStrings = new String[]
   {
      "Page  0", "Page  1", "Page  2", "Page  3"
   };

   /* single-instance viewer tasks */
   private final ViewerTask readScratchpadTask = new ReadScratchpadTask();
   private final ViewerTask readDataPageTask = new ReadDataPageTask();
   private final ViewerTask writeProtectMemoryTask = new WriteProtectMemoryTask();

   /**
    * Constructs a new SHA33Viewer for integration in the OneWireViewer
    */
   public SHA33Viewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 3;

      // Combo box showing available pages
      pageList = new JList(pageStrings);
      pageList.setFont(fontPlain);
      pageList.setVisibleRowCount(4);
      pageList.addListSelectionListener(new ListSelectionListener()
         {
            public void valueChanged(ListSelectionEvent lse)
            {
               if(lse.getValueIsAdjusting()||pageList.getSelectedIndex()<0)
                  return;

               txtTargetPage.setText(""+pageList.getSelectedIndex());
               txtTargetOffset.setText("0");
               txtDataPage.setText("");
            }
         }
      );

      // -- North
      // page panel
      JScrollPane pageScroll = new JScrollPane(pageList);
      pageScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Memory Pages"));

      // -- Center
      // Another BorderLayout JPanel
      JPanel centerPanel = new JPanel(new BorderLayout(3,3));

         // SHA scratchpad and page data
         JPanel dataPanel = new JPanel(new GridLayout(2,1));
         dataPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Memory Contents"));
            // registers
            JPanel regPanel = new JPanel(new BorderLayout());
               //TA1/TA2 E/S
               JPanel targetPanel = new JPanel();
               targetPanel.setLayout(new BoxLayout(targetPanel, BoxLayout.X_AXIS));
                  JLabel targetPage = new JLabel(" Target Page ");
                  txtTargetPage = new JTextField();
                  txtTargetPage.setFont(fontBold);
                  JLabel targetOffset = new JLabel("  Offset ");
                  txtTargetOffset = new JTextField();
                  txtTargetOffset.setFont(fontBold);
               targetPanel.add(targetPage);
               targetPanel.add(txtTargetPage);
               targetPanel.add(targetOffset);
               targetPanel.add(txtTargetOffset);
               // scratchpad
               JPanel scratchpadPanel = new JPanel(new BorderLayout());
                  JLabel scratchpadLabel = new JLabel("Scratchpad ");
                  scratchpadLabel.setFont(fontBold);
                  txtScratchpad = new JTextArea();
                  txtScratchpad.setFont(fontBold);
               scratchpadPanel.add(scratchpadLabel, BorderLayout.NORTH);
               scratchpadPanel.add(new JScrollPane(txtScratchpad), BorderLayout.CENTER);
            regPanel.add(targetPanel, BorderLayout.NORTH);
            regPanel.add(scratchpadPanel, BorderLayout.CENTER);
            // data page
            JPanel dataPagePanel = new JPanel(new BorderLayout());
               JLabel dataPageLabel = new JLabel("Data Page ");
               dataPageLabel.setFont(fontBold);
               txtDataPage = new JTextArea();
               txtDataPage.setFont(fontBold);
            dataPagePanel.add(dataPageLabel, BorderLayout.NORTH);
            dataPagePanel.add(new JScrollPane(txtDataPage), BorderLayout.CENTER);
         dataPanel.add(regPanel);
         dataPanel.add(dataPagePanel);

         // Command Panel
         JPanel commandPanel = new JPanel();
         commandPanel.setLayout(new BoxLayout(commandPanel, BoxLayout.Y_AXIS));
         JScrollPane commandScroll = new JScrollPane(commandPanel);
         commandScroll.setBorder(BorderFactory.createTitledBorder(
                           BorderFactory.createEtchedBorder(), "Command"));
            JButton tmpButton = new JButton("Read Scratchpad");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     enqueueRunTask(readScratchpadTask);
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Write Scratchpad");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        try
                        {
                           byte[] scratchpad = Convert.toByteArray(txtScratchpad.getText());
                           int targetPage = Integer.parseInt(txtTargetPage.getText());
                           int targetOffset = Integer.parseInt(txtTargetOffset.getText());
                           enqueueRunTask(new WriteScratchpadTask(adapter,
                              container, targetPage, targetOffset,
                              scratchpad, scratchpad.length));
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        catch(NumberFormatException nfe)
                        {
                           setStatus(ERRMSG, "Error during conversion: " + nfe);
                        }
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Copy Scratchpad");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     int targetPage = 0, targetOffset = 0;
                     try
                     {
                        targetPage = Integer.parseInt(txtTargetPage.getText());
                        targetOffset = Integer.parseInt(txtTargetOffset.getText());
                     }
                     catch(NumberFormatException nfe)
                     {
                        nfe.printStackTrace();
                     }
                     enqueueRunTask(new CopyScratchpadTask(adapter, container,
                                                           targetPage, targetOffset));
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Copy Scratchpad w/ MAC");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     JPanel panel = new JPanel(new BorderLayout());
                     panel.add(
                        new JLabel("Message Authentication Code (20 bytes, in Hex)"),
                        BorderLayout.NORTH);
                     JTextField MAC = new JTextField();
                     panel.add(MAC, BorderLayout.SOUTH);
                     int ans = JOptionPane.showConfirmDialog(SHA33Viewer.this, panel,
                           "MAC",JOptionPane.OK_CANCEL_OPTION);
                     if(ans!=JOptionPane.CANCEL_OPTION)
                     {
                        try
                        {
                           byte[] bMAC = Convert.toByteArray(MAC.getText());
                           if(bMAC.length!=20)
                              setStatus(ERRMSG, "MAC must be 20 bytes.  The provided MAC was " + bMAC.length + " bytes.");
                           else
                              enqueueRunTask(
                                 new CopyScratchpadMACTask(adapter, container, bMAC));
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Read Data Page");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     enqueueRunTask(readDataPageTask);
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Load First Secret");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     JPanel panel = new JPanel(new BorderLayout());
                     panel.add(new JLabel("Secret (8 bytes, in Hex)"), BorderLayout.NORTH);
                     JTextField secret = new JTextField();
                     panel.add(secret, BorderLayout.SOUTH);
                     int ans = JOptionPane.showConfirmDialog(SHA33Viewer.this, panel,
                           "Secret",JOptionPane.OK_CANCEL_OPTION);
                     if(ans==JOptionPane.CANCEL_OPTION)
                        return;
                     try
                     {
                        byte[] bSecret = new byte[8];
                        Convert.toByteArray(secret.getText(), bSecret);
                        enqueueRunTask(new LoadFirstSecretTask(adapter,
                              container, -1, bSecret));
                     }
                     catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                     {
                        setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                     }
                     catch(NumberFormatException nfe)
                     {
                        setStatus(ERRMSG, "Error during conversion: " + nfe);
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
         if(ViewerProperties.getPropertyBoolean("DS1961S.LoadFirstSecretWithAddress", false))
         {
               tmpButton = new JButton("Load First Secret W/ Addr");
               tmpButton.setFont(fontBold);
               tmpButton.addActionListener(new ActionListener()
                  {
                     public void actionPerformed(ActionEvent ae)
                     {
                        JPanel panel1 = new JPanel(new BorderLayout());
                        panel1.add(new JLabel("Secret (8 bytes, in Hex)"), BorderLayout.NORTH);
                        JTextField secret = new JTextField();
                        panel1.add(secret, BorderLayout.SOUTH);
   
                        JPanel panel2 = new JPanel(new BorderLayout());
                        panel2.add(new JLabel("Address (1 byte, in Hex)"), BorderLayout.NORTH);
                        JTextField address = new JTextField();
                        panel2.add(address, BorderLayout.SOUTH);
                        
                        JPanel panel = new JPanel(new BorderLayout(5, 5));
                        panel.add(panel1, BorderLayout.SOUTH);
                        panel.add(panel2, BorderLayout.NORTH);
   
                        int ans = JOptionPane.showConfirmDialog(SHA33Viewer.this, panel,
                              "Secret",JOptionPane.OK_CANCEL_OPTION);
                        if(ans==JOptionPane.CANCEL_OPTION)
                           return;
                        
                        try
                        {
                           byte[] bSecret = new byte[8];
                           int bAddr = (0x0FF&Integer.parseInt(address.getText(), 16));
                           System.out.println("bAddr=" + bAddr);
                           Convert.toByteArray(secret.getText(), bSecret);
                           enqueueRunTask(new LoadFirstSecretTask(adapter,
                                 container, bAddr, bSecret));
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                     }
                  }
               );
            commandPanel.add(tmpButton);
         }
            tmpButton = new JButton("Refresh Page");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        try
                        {
                           int targetPage = Integer.parseInt(txtTargetPage.getText());
                           enqueueRunTask(new RefreshPageTask(adapter,
                              container, targetPage));
                        }
                        catch(NumberFormatException nfe)
                        {
                           setStatus(ERRMSG, "Please select a Target Page");
                        }
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Compute Next Secret");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     int pgNum = pageList.getSelectedIndex();
                     if(pgNum<0)
                        return;
                     enqueueRunTask(new ComputeNextSecretTask(adapter,
                           container, pgNum));
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Write-Protect Memory");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     enqueueRunTask(writeProtectMemoryTask);
                  }
               }
            );
         commandPanel.add(tmpButton);

         // extra information panel
         JPanel extraInfoPanel = new JPanel(new BorderLayout(3,3));
         extraInfoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Extra Info"));
            //MAC
            JPanel macPanel = new JPanel();
            macPanel.setLayout(new BoxLayout(macPanel, BoxLayout.X_AXIS));
               JLabel macLabel = new JLabel("MAC ", JLabel.RIGHT);
               macLabel.setFont(fontBold);
               macLabel.setToolTipText("Message Authentication Code ");
               txtMac = new JTextField();
               txtMac.setFont(fontBold);
            macPanel.add(macLabel);
            macPanel.add(txtMac);
            //Container Secret
            JPanel secretPanel = new JPanel();
            secretPanel.setLayout(new BoxLayout(secretPanel, BoxLayout.X_AXIS));
               JLabel secretLabel = new JLabel("Container Secret ", JLabel.RIGHT);
               secretLabel.setFont(fontBold);
               txtSecret = new JTextField();
               txtSecret.setFont(fontBold);
               JButton secretButton = new JButton("Set Container Secret");
               secretButton.setFont(fontBold);
               secretButton.addActionListener(new ActionListener()
                  {
                     public void actionPerformed(ActionEvent ae)
                     {
                        synchronized(syncObj)
                        {
                           if(container!=null)
                           {
                              try
                              {
                                 byte[] secret = new byte[8];
                                 if(Convert.toByteArray(txtSecret.getText(), secret)==8)
                                    container.setContainerSecret(secret, 0);
                              }
                              catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                              {
                                 setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                              }
                           }
                        }
                     }
                  }
               );
            secretPanel.add(secretLabel);
            secretPanel.add(txtSecret);
            secretPanel.add(secretButton);
         extraInfoPanel.add(macPanel, BorderLayout.NORTH);
         extraInfoPanel.add(secretPanel, BorderLayout.SOUTH);

      centerPanel.add(dataPanel, BorderLayout.CENTER);
      centerPanel.add(commandScroll, BorderLayout.EAST);
      centerPanel.add(extraInfoPanel, BorderLayout.SOUTH);

      add(pageScroll, BorderLayout.NORTH);
      add(centerPanel, BorderLayout.CENTER);

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
      return (owc!=null) && (owc instanceof OneWireContainer33);
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
      return (td!=null) && containerSupported(td.getDeviceContainer());
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
            this.container = (OneWireContainer33)owc;
            this.romID = owc.getAddressAsString();;
         }
      }
   }

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public void setContainer(TaggedDevice td)
   {
      clearContainer();
      if(td!=null)
         setContainer(td.getDeviceContainer());
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
      }
      clearFields();
   }

   /**
    * Clears all text areas and text fields
    */
   private void clearFields()
   {
      txtScratchpad.setText("");
      txtDataPage.setText("");
      txtMac.setText("");
      txtSecret.setText("");
   }

   public void run()
   {
      while(executeRunTask())
         /* no-op */;
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
      return false;
   }

   /**
    * Create a complete clone of this viewer, including reference to
    * current container.  Used to display viewer in new window.
    *
    * @return a clone of the current viewer
    */
   public Viewer cloneViewer()
   {
      SHA33Viewer v = new SHA33Viewer();
      v.setContainer(container);
      return v;
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
    * Gets the string that represents this Viewer's title
    *
    * @return viewer's title
    */
   public String getViewerTitle()
   {
      if(ViewerProperties.getPropertyBoolean(
         OneWireViewer.SHOW_ALTERNATE_NAMES, false))
         return strTabAlt;
      else
         return strTab;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * ReadScratchpadTask encapsulates the action of reading the scratchpad of
    * a DS1961S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadScratchpadTask extends ViewerTask
   {
      private final byte[] scratchpad = new byte[8];
      private final StringBuffer buffer = new StringBuffer(100);
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer33 l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }
         setStatus(VERBOSE, "Reading Scratchpad...");
         try
         {
            l_adapter.beginExclusive(true);
            l_container.readScratchpad(scratchpad, 0, null);
            txtScratchpad.setText(Convert.toHexString(scratchpad, 0, 8, " "));
            setStatus(VERBOSE, "Done reading.");
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
    * WriteScratchpadTask encapsulates the action of reading the scratchpad of
    * a DS1961S SHA Device.
    */
   protected class WriteScratchpadTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      int task_page, task_offset, task_len;
      byte[] task_data;

      public WriteScratchpadTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            int targetPage, int targetOffset,
                            byte[] data, int len)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_page = targetPage;
         task_offset = targetOffset;
         task_data = data;
         task_len = len;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Writing Scratchpad...");
         try
         {
            task_adapter.beginExclusive(true);

            if(task_len>7)
            {
               task_container.setChallenge(task_data, 4);
            }
            if(task_container.writeScratchpad(task_page, task_offset,
                                              task_data, 0, task_len))
               setStatus(VERBOSE, "Done Writing.");
            else
               setStatus(ERROR, "Error writing device! ");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error writing device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * RefreshPageTask encapsulates the action of refreshing the scratchpad of
    * each 8-byte block in a page of the DS1961S, and re-writing that block
    * back to the memory.
    */
   protected class RefreshPageTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      int task_page;

      public RefreshPageTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            int targetPage)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_page = targetPage;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Refreshing Page...");
         try
         {
            task_adapter.beginExclusive(true);

            if(task_container.refreshPage(task_page))
            {
               setStatus(VERBOSE, "Done Refreshing.");
            }
            else
            {
               setStatus(ERROR, "Error Refreshing Page of device! ");
            }
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error writing device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }
   
   /**
    * CopyScratchpadTask encapsulates the action of copying the scratchpad of
    * a DS1961S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class CopyScratchpadTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      int task_page, task_offset, task_len;
      public CopyScratchpadTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            int targetPage, int targetOffset)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_page = targetPage;
         task_offset = targetOffset;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Copying Scratchpad...");
         try
         {
            task_adapter.beginExclusive(true);

            if(task_container.copyScratchpad(task_page, task_offset))
               setStatus(VERBOSE, "Done Copying.");
            else
               setStatus(ERROR, "Error copying to device! ");
         }
         catch(Exception e)
         {
            e.printStackTrace();
            setStatus(ERROR, "Error copying to device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * CopyScratchpadTask encapsulates the action of copying the scratchpad of
    * a DS1961S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class CopyScratchpadMACTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      byte[] task_mac;

      public CopyScratchpadMACTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            byte[] l_mac)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_mac = l_mac;
      }

      public void executeTask()
      {
         byte[] scratchpad = new byte[8];
         byte[] extraInfo = new byte[3];
         setStatus(VERBOSE, "Copying Scratchpad using MAC...");
         try
         {
            task_adapter.beginExclusive(true);

            task_container.readScratchpad(scratchpad, 0, extraInfo);
            int TA = (extraInfo[0]<<8)|extraInfo[1];
            if(task_container.copyScratchpad(TA/32, TA%32, task_mac, 0))
               setStatus(VERBOSE, "Done Copying.");
            else
               setStatus(ERROR, "Error copying to device! ");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error copying to device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * ReadDataPageTask encapsulates the action of reading the data page of
    * a DS1961S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadDataPageTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         try
         {
            OneWireContainer33 l_container = null;
            int pageNum = -1;
            synchronized(syncObj)
            {
               if(adapter==null || container==null)
                  return;
               l_adapter = adapter;
               l_container = container;
               pageNum = pageList.getSelectedIndex();
               if(pageNum<0)
                  return;
            }
            byte[] dataPage = new byte[52];
            StringBuffer buffer = new StringBuffer(100);
   
            setStatus(VERBOSE, "Reading Data Page...");
            l_adapter.beginExclusive(true);
            l_container.readAuthenticatedPage(pageNum, dataPage, 0, dataPage, 32);
            buffer.delete(0, buffer.length());
            for(int i=0; i<32; i+=8)
            {
               buffer.append(Convert.toHexString(dataPage, i, 8, " "));
               buffer.append("\n");
            }
            txtDataPage.setText(buffer.toString());

            txtMac.setText(Convert.toHexString(dataPage, 32, 20, " "));

            setStatus(VERBOSE, "Done reading.");
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
    * LoadFirstSecretTask encapsulates the action of using the authenticate host
    * command of a DS1961S SHA Device.
    */
   protected class LoadFirstSecretTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      byte[] task_secret;
      int task_addr;

      public LoadFirstSecretTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            int l_addr, byte[] l_secret)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_secret = l_secret;
         task_addr = l_addr;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Computing Secret...");
         try
         {
            task_adapter.beginExclusive(true);

            if(task_addr==-1 || task_addr==0x080)
            {
               task_container.loadFirstSecret(task_secret, 0);
               txtSecret.setText(Convert.toHexString(task_secret, 0, 8, " "));
            }
            else
               task_container.getScratchpadMemoryBank().loadFirstSecret(
                  task_addr, task_secret, 0);

            setStatus(VERBOSE, "Done Loading First Secret.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error Loading First Secret! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }
  /**
    * ComputeNextSecretTask encapsulates the action of using the Compute Next
    * Secret command of a DS1961S SHA Device.
    */
   protected class ComputeNextSecretTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer33 task_container = null;
      int task_pgNum;

      public ComputeNextSecretTask(DSPortAdapter l_adapter,
                            OneWireContainer33 l_container,
                            int l_pgNum)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_pgNum = l_pgNum;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Computing Secret...");
         try
         {
            task_adapter.beginExclusive(true);

            task_container.computeNextSecret(task_pgNum);

            if(task_container.isContainerSecretSet())
            {
               byte[] secret = new byte[8];
               task_container.getContainerSecret(secret, 0);
               txtSecret.setText(Convert.toHexString(secret, 0, 8, " "));
            }
            setStatus(VERBOSE, "Done Computing Next Secret.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error computing next secret! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }
   
   /**
    * Write-Protect Memory Task
    * Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class WriteProtectMemoryTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer33 l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }
         try
         {
            l_adapter.beginExclusive(true);
            boolean isWP0 = l_container.isWriteProtectPageZeroSet();
            boolean isWPAll = l_container.isWriteProtectAllSet();
            boolean isWPSec = l_container.isSecretWriteProtected();
            
            int optionCount = 0;
            if(!isWP0)
               optionCount++;
            if(!isWPAll)
               optionCount++;
            if(!isWPSec)
               optionCount++;
            
            String title = "Select area of memory to write-protect.";
            Object[] options = new Object[optionCount + 1];
            optionCount = 0;
            if(!isWP0)
               options[optionCount++] = "Page 0 only";
            else
               title += "\r\nPage 0 is write-protected.";
            if(!isWPAll)
               options[optionCount++] = "Pages 0-3";
            else
               title += "\r\nPages 0-3 are write-protected.";
            if(!isWPSec)
               options[optionCount++] = "Secret";
            else
               title += "\r\nSecret is write-protected.";
            options[optionCount] = "Cancel";

            int optionIndex = 
               JOptionPane.showOptionDialog(null, 
                  title, 
                  "Write-Protect Memory", 0, 
                  JOptionPane.QUESTION_MESSAGE, 
                  null, options, null);
            if(optionIndex>=0 && optionIndex<optionCount)
            {
               String option = (String)options[optionIndex];
               setStatus(VERBOSE, "Write-Protecting " + option + "...");
               if(option.equals("Page 0 only"))
               {
                  l_container.writeProtectPageZero();
               }
               else if(option.equals("Pages 0-3"))
               {
                  l_container.writeProtectAll();
               }
               else if(option.equals("Secret"))
               {
                  l_container.writeProtectSecret();
               }
               setStatus(VERBOSE, "Done reading.");
            }
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error write-protectecting device! "+e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

}