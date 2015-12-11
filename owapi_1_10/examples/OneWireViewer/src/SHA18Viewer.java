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
import com.dalsemi.onewire.container.OneWireContainer18;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.utils.Convert;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;

/**
 * A DS1963S (family code 0x18) viewer for integration in the OneWireViewer.
 * This viewer allows the user to manipulate the memory of the DS1963S iButton
 * as well as exercise all of the SHA specific features of the part.
 *
 * @author SH
 * @version 1.00
 */
public class SHA18Viewer extends Viewer
   implements Runnable
{
   /* string constants */
   private static final String strTitle = "SHA18Viewer";
   private static final String strTab = "DS1963S SHA";
   private static final String strTip = "Exercises SHA functionality of DS1963S";
   private static final String naString = "N/A";

   /* container variables */
   private OneWireContainer18 container  = null;
   private TaggedDevice taggedDevice = null;

   /* visual components */
   private JTextField txtTargetPage = null, txtTargetOffset = null;
   private JTextField txtMac, txtWcc = null, txtSwcc = null;
   private JTextArea txtScratchpad = null, txtDataPage = null;
   private JList pageList = null;
   private String[] pageStrings = new String[]
   {
      "Page  0 (Secret 0)", "Page  1 (Secret 1)", "Page  2 (Secret 2)",
      "Page  3 (Secret 3)", "Page  4 (Secret 4)", "Page  5 (Secret 5)",
      "Page  6 (Secret 6)", "Page  7 (Secret 7)",
      "Page  8 (Secret 0, WCC 0)", "Page  9 (Secret 1, WCC 1)",
      "Page 10 (Secret 2, WCC 2)", "Page 11 (Secret 3, WCC 3)",
      "Page 12 (Secret 4, WCC 4)", "Page 13 (Secret 5, WCC 5)",
      "Page 14 (Secret 6, WCC 6)", "Page 15 (Secret 7, WCC 7)"
   };

   /* single-instance viewer tasks */
   private final ViewerTask readScratchpadTask = new ReadScratchpadTask();
   private final ViewerTask eraseScratchpadTask = new EraseScratchpadTask();
   private final ViewerTask readDataPageTask = new ReadDataPageTask();

   /**
    * Constructs a new SHA18Viewer for integration in the OneWireViewer
    */
   public SHA18Viewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 2;

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

               clearFields();
               txtTargetPage.setText(""+pageList.getSelectedIndex());
               txtTargetOffset.setText("0");
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
                        byte[] scratchpad = new byte[32];
                        int len = -1; 
                        int targetPage = pageList.getSelectedIndex(), targetOffset = 0;
                        try
                        {
                           len = Convert.toByteArray(txtScratchpad.getText(), scratchpad, 0, 32);
                           targetPage = Integer.parseInt(txtTargetPage.getText());
                           targetOffset = Integer.parseInt(txtTargetOffset.getText());
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        catch(NumberFormatException nfe)
                        {
                           setStatus(ERRMSG, "Error during conversion: " + nfe);
                        }
                        if(len>0)
                           enqueueRunTask(new WriteScratchpadTask(adapter,
                                 container, targetPage, targetOffset,
                                 scratchpad, len));
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
                     enqueueRunTask(new CopyScratchpadTask());
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Match Scratchpad");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     String strMac = JOptionPane.showInputDialog(
                           SHA18Viewer.this,
                           new JLabel("Enter MAC for Match Scratchpad"),
                           "Match Scratchpad", JOptionPane.OK_CANCEL_OPTION);
                     if(strMac==null)
                        return;

                     try
                     {
                        byte[] mac = new byte[20];
                        int len = Convert.toByteArray(strMac, mac, 0, 20);
                        synchronized(syncObj)
                        {
                           if(len==20)
                           {
                              enqueueRunTask(new MatchScratchpadTask(adapter,
                                    container, mac));
                           }
                        }
                     }
                     catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                     {
                        setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Erase Scratchpad");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     enqueueRunTask(eraseScratchpadTask);
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
            tmpButton = new JButton("Validate Data Page");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        int pgNum = pageList.getSelectedIndex();
                        if(pgNum<0)
                           return;
                        enqueueRunTask(new ValidateDataPageTask(adapter,
                              container, pgNum));
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Sign Data Page");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        int pgNum = pageList.getSelectedIndex();
                        if(pgNum<0)
                           return;
                        if(pgNum!=0 && pgNum!=8)
                        {
                           JOptionPane.showMessageDialog(SHA18Viewer.this,
                              new JLabel("Sign Data Page only works on page 0 or 8"));
                        }
                        else
                        {
                           enqueueRunTask(new SignDataPageTask(adapter,
                                 container, pgNum));
                           enqueueRunTask(readScratchpadTask);
                        }
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Authenticate Host");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        int pgNum = pageList.getSelectedIndex();
                        if(pgNum<0)
                           return;
                        enqueueRunTask(new AuthenticateHostTask(adapter,
                              container, pgNum));
                     }
                  }
               }
            );
         commandPanel.add(tmpButton);
            tmpButton = new JButton("Compute First Secret");
            tmpButton.setFont(fontBold);
            tmpButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     int pgNum = pageList.getSelectedIndex();
                     if(pgNum<0)
                        return;
                     JPanel panel = new JPanel(new BorderLayout());
                     panel.add(new JLabel("Target Secret"), BorderLayout.NORTH);
                     JTextField secret = new JTextField((pageList.getSelectedIndex()&7)+"");
                     panel.add(secret, BorderLayout.SOUTH);
                     int ans = JOptionPane.showConfirmDialog(SHA18Viewer.this, panel,
                           "Target Secret",JOptionPane.OK_CANCEL_OPTION);
                     if(ans==JOptionPane.CANCEL_OPTION)
                        return;
                     int secretNum = -1;
                     try
                     {
                        secretNum = Integer.parseInt(secret.getText());
                     }
                     catch(NumberFormatException nfe)
                     {;}
                     if(secretNum<0 || secretNum>7)
                     {
                        JOptionPane.showMessageDialog(SHA18Viewer.this,
                           new JLabel("Must enter integer between 0 and 8"));
                     }
                     else
                     {
                        enqueueRunTask(new ComputeSecretTask(adapter,
                              container, pgNum, secretNum, true));
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
                     JPanel panel = new JPanel(new BorderLayout());
                     panel.add(new JLabel("Target Secret"), BorderLayout.NORTH);
                     JTextField secret = new JTextField((pageList.getSelectedIndex()&7)+"");
                     panel.add(secret, BorderLayout.SOUTH);
                     int ans = JOptionPane.showConfirmDialog(SHA18Viewer.this, panel,
                           "Target Secret",JOptionPane.OK_CANCEL_OPTION);
                     if(ans==JOptionPane.CANCEL_OPTION)
                        return;
                     int secretNum = -1;
                     try
                     {
                        secretNum = Integer.parseInt(secret.getText());
                     }
                     catch(NumberFormatException nfe)
                     {;}
                     if(secretNum<0 || secretNum>7)
                     {
                        JOptionPane.showMessageDialog(SHA18Viewer.this,
                           new JLabel("Must enter integer between 0 and 8"));
                     }
                     else
                     {
                        enqueueRunTask(new ComputeSecretTask(adapter,
                              container, pgNum, secretNum, false));
                     }
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
            //WCC & Secret WCC
            JPanel wccPanel = new JPanel();
            wccPanel.setLayout(new BoxLayout(wccPanel, BoxLayout.X_AXIS));
               JLabel wccLabel = new JLabel("WCC ", JLabel.RIGHT);
               wccLabel.setFont(fontBold);
               wccLabel.setToolTipText("Write-Cycle Counter ");
               txtWcc = new JTextField();
               txtWcc.setFont(fontBold);
               txtWcc.setEditable(false);
               JLabel swccLabel = new JLabel("   Secret WCC ", JLabel.RIGHT);
               swccLabel.setFont(fontBold);
               swccLabel.setToolTipText("Secret Write-Cycle Counter ");
               txtSwcc = new JTextField();
               txtSwcc.setFont(fontBold);
               txtSwcc.setEditable(false);
            wccPanel.add(wccLabel);
            wccPanel.add(txtWcc);
            wccPanel.add(swccLabel);
            wccPanel.add(txtSwcc);
         extraInfoPanel.add(macPanel, BorderLayout.NORTH);
         extraInfoPanel.add(wccPanel, BorderLayout.SOUTH);

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
      return (owc!=null) && (owc instanceof OneWireContainer18);
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
            this.container = (OneWireContainer18)owc;
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
      // not yet supported as a tagged device
      return;
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
      txtWcc.setText("");
      txtSwcc.setText("");
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
      SHA18Viewer v = new SHA18Viewer();
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
      return strTab;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * ReadScratchpadTask encapsulates the action of reading the scratchpad of
    * a DS1963S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadScratchpadTask extends ViewerTask
   {
      private final byte[] scratchpad = new byte[32];
      private final StringBuffer buffer = new StringBuffer(100);
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer18 l_container = null;
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
            l_container.readScratchPad(scratchpad, 0);
            buffer.delete(0, buffer.length());
            for(int i=0; i<32; i+=8)
            {
               buffer.append(Convert.toHexString(scratchpad, i, 8, " "));
               buffer.append("\n");
            }
            txtScratchpad.setText(buffer.toString());
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
    * a DS1963S SHA Device.
    */
   protected class WriteScratchpadTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      int task_page, task_offset, task_len;
      byte[] task_data;

      public WriteScratchpadTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
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

            if(task_container.writeScratchPad(task_page, task_offset,
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
    * WriteScratchpadTask encapsulates the action of reading the scratchpad of
    * a DS1963S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class CopyScratchpadTask extends ViewerTask
   {
      private final byte[] scratchpad = new byte[32];
      private final StringBuffer buffer = new StringBuffer(100);
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer18 l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }
         setStatus(VERBOSE, "Copying Scratchpad...");
         try
         {
            l_adapter.beginExclusive(true);
            l_container.readScratchPad(scratchpad, 0);
            if(l_container.copyScratchPad())
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
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * ReadScratchpadTask encapsulates the action of reading the scratchpad of
    * a DS1963S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class EraseScratchpadTask extends ViewerTask
   {
      private final byte[] scratchpad = new byte[32];
      private final StringBuffer buffer = new StringBuffer(100);
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer18 l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         setStatus(VERBOSE, "Erasing Scratchpad...");
         try
         {
            l_adapter.beginExclusive(true);
            l_container.eraseScratchPad(0);
            setStatus(VERBOSE, "Done erasing.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error erasing device! "+e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * MatchScratchpadTask encapsulates the action of matching the scratchpad of
    * a DS1963S SHA Device.
    */
   protected class MatchScratchpadTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      byte[] task_mac;

      public MatchScratchpadTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
                            byte[] mac)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_mac = mac;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Matching Scratchpad...");
         try
         {
            task_adapter.beginExclusive(true);
            if(task_container.matchScratchPad(task_mac))
               setStatus(VERBOSE, "Match Successful.");
            else
               setStatus(VERBOSE, "Match Unsuccessful: "
                          + Convert.toHexString(task_mac, 0, 20, " "));
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error matching device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * ReadDataPageTask encapsulates the action of reading the data page of
    * a DS1963S SHA Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadDataPageTask extends ViewerTask
   {
      private final byte[] dataPage = new byte[42];
      private final StringBuffer buffer = new StringBuffer(100);
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer18 l_container = null;
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

         setStatus(VERBOSE, "Reading Data Page...");
         try
         {
            l_adapter.beginExclusive(true);
            l_container.readAuthenticatedPage(pageNum, dataPage, 0);
            l_container.useResume(true);
            buffer.delete(0, buffer.length());
            for(int i=0; i<32; i+=8)
            {
               buffer.append(Convert.toHexString(dataPage, i, 8, " "));
               buffer.append("\n");
            }
            txtDataPage.setText(buffer.toString());
            txtWcc.setText(Convert.toHexString(dataPage, 32, 4, " "));
            txtSwcc.setText(Convert.toHexString(dataPage, 36, 4, " "));

            l_container.readScratchPad(dataPage, 0);
            txtMac.setText(Convert.toHexString(dataPage, 8, 20, " "));

            setStatus(VERBOSE, "Done reading.");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error reading device! "+e.toString());
         }
         finally
         {
            l_container.useResume(false);
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * ValidateDataPageTask encapsulates the action of validating the data page of
    * a DS1963S SHA Device.
    */
   protected class ValidateDataPageTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      int task_pgNum;

      public ValidateDataPageTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
                            int l_pgNum)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_pgNum = l_pgNum;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Validating Data Page...");
         try
         {
            task_adapter.beginExclusive(true);
            if(task_container.SHAFunction(OneWireContainer18.VALIDATE_DATA_PAGE,
                                       task_pgNum<<5))
               setStatus(VERBOSE, "SHA Function Validate Data Page Successful!");
            else
               setStatus(VERBOSE, "SHA Function unsuccessful!");

         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error matching device! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * SignDataPageTask encapsulates the action of signing page 0 or 8 of
    * a DS1963S SHA Device.
    */
   protected class SignDataPageTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      int task_pgNum;

      public SignDataPageTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
                            int l_pgNum)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_pgNum = l_pgNum;
      }

      public void executeTask()
      {
         //byte[] scratchpad = new byte[32];
         setStatus(VERBOSE, "Signing Data Page...");
         try
         {
            task_adapter.beginExclusive(true);
            //task_container.eraseScratchPad(0);
            if(task_container.SHAFunction(OneWireContainer18.SIGN_DATA_PAGE,
                                       task_pgNum<<5))
               setStatus(VERBOSE, "SHA Function Sign Data Page Successful!");
            else
               setStatus(VERBOSE, "SHA Function unsuccessful!");
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error signing! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * AuthenticateHostTask encapsulates the action of using the authenticate host
    * command of a DS1963S SHA Device.
    */
   protected class AuthenticateHostTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      int task_pgNum;

      public AuthenticateHostTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
                            int l_pgNum)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_pgNum = l_pgNum;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Authenticating Host...");
         try
         {
            task_adapter.beginExclusive(true);
            if(task_container.SHAFunction(OneWireContainer18.AUTH_HOST,
                                       task_pgNum<<5))
               setStatus(VERBOSE, "SHA Function Authenticate Host Successful!");
            else
               setStatus(VERBOSE, "SHA Function unsuccessful!");

         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error authenticating host! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }


   /**
    * ComputeSecretTask encapsulates the action of using the authenticate host
    * command of a DS1963S SHA Device.
    */
   protected class ComputeSecretTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer18 task_container = null;
      int task_pgNum, task_secretNum;
      boolean task_computeFirst;

      public ComputeSecretTask(DSPortAdapter l_adapter,
                            OneWireContainer18 l_container,
                            int l_pgNum, int l_secretNum,
                            boolean l_computeFirst)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_pgNum = l_pgNum;
         task_secretNum = l_secretNum;
         task_computeFirst = l_computeFirst;
      }

      public void executeTask()
      {
         setStatus(VERBOSE, "Computing Secret...");
         try
         {
            task_adapter.beginExclusive(true);

            if(task_computeFirst)
               task_container.SHAFunction(OneWireContainer18.COMPUTE_FIRST_SECRET,
                                          task_pgNum<<5);
            else
               task_container.SHAFunction(OneWireContainer18.COMPUTE_NEXT_SECRET,
                                          task_pgNum<<5);
            // find which page the target secret is on
            int secret_page = (task_secretNum > 3) ? 17 : 16;
            // each page has 4 secrets, so look at 2 LS bits
            int secret_offset = (task_secretNum & 3) << 3;

            byte[] nullBytes = new byte[32];
            if(task_container.writeScratchPad(secret_page, secret_offset,
                                              nullBytes, 0, 8))
            {
               task_container.readScratchPad(nullBytes, 0);
               if(task_container.copyScratchPad())
                  setStatus(VERBOSE, "Done Copying Secret.");
               else
                  setStatus(ERROR, "Error copying secret to device! ");
            }
            else
            {
                setStatus(VERBOSE, "Writing scratchpad with 8 dummy bytes failed");
            }
         }
         catch(Exception e)
         {
            setStatus(ERROR, "Error computing first secret! "+e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }
}