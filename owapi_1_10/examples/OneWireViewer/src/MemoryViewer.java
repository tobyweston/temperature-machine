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
import java.util.*;

import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.MemoryBank;
import com.dalsemi.onewire.container.PagedMemoryBank;
import com.dalsemi.onewire.container.OTPMemoryBank;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.utils.Convert;


/**
 * A 1-Wire Device Memory Viewer for integration in the OneWireViewer.
 * All devices that contain <CODE>MemoryBank</CODE>s are supported by
 * this viewer.  This viewer creates a list of each bank of memory.
 * When a bank is selected, all info about that bank of memory and its
 * entire contents are displayed.
 *
 * @author SH
 * @version 1.00
 */
public class MemoryViewer extends Viewer
   implements Runnable
{
   /* string constants */
   private static final String strTitle = "MemoryViewer";
   private static final String strTab = "Memory";
   private static final String strTip = "Displays raw memory contents";

   /* container variables */
   private OneWireContainer container  = null;
   private TaggedDevice taggedDevice = null;

   /* visual components */
   private JList bankList = null;
   private DefaultListModel bankListModel = null;
   private Vector memoryBanks = null;
   private BytePanel bytePanel = null;

   private volatile byte[] cachedReadBytes = null;
   private MemoryBank cachedReadBank = null;
   private int lastIndex = -1;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = { "Start Address ",
                                  "Bank Size ",
                                  "Is General Purpose? ",
                                  "Is Non-Volatile? ",
                                  "Is Read-Only? ",
                                  "Is Read-Write? ",
                                  "Is Write-Once? " };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 7;
   private static final int START=0;
   private static final int SIZE=1;
   private static final int IS_GP=2;
   private static final int IS_NV=3;
   private static final int IS_RO=4;
   private static final int IS_RW=5;
   private static final int IS_OTP=6;

   private byte[] readBuf = new byte[32];

   /* single-instance Viewer Tasks */
   protected final ViewerTask readMemoryTask = new ReadMemoryTask();
   protected final ViewerTask readFeaturesTask = new ReadFeaturesTask();

   /**
    * Constructs a new instance of MemoryViewer.
    */
   public MemoryViewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 3;

      memoryBanks = new Vector();

      // feature panel
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
         lblFeatureHdr[i].setFont(Viewer.fontBold);
         lblFeatureHdr[i].setForeground(Color.black);
         lblFeatureHdr[i].setBackground(Color.lightGray);

         lblFeature[i] = new JLabel("", JLabel.LEFT);
         lblFeature[i].setOpaque(true);
         lblFeature[i].setFont(Viewer.fontPlain);
         lblFeature[i].setForeground(Color.black);
         lblFeature[i].setBackground(Color.lightGray);

         featurePanel.add(lblFeatureHdr[i]);
         featurePanel.add(lblFeature[i]);
      }

      // memory banks
      bankListModel = new DefaultListModel();
      bankList = new JList(bankListModel);
      bankList.setFont(fontPlain);
      bankList.setVisibleRowCount(4);
      bankList.addListSelectionListener(new ListSelectionListener()
         {
            public void valueChanged(ListSelectionEvent lse)
            {
               if(lse.getValueIsAdjusting())
                  return;
               bytePanel.clearBytes();
               enqueueRunTask(readMemoryTask);
               enqueueRunTask(readFeaturesTask);
            }
         }
      );
      JScrollPane bankScroll = new JScrollPane(bankList,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      bankScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Banks"));

      // Contents
      bytePanel = new BytePanel();
      JScrollPane contentScroll = new JScrollPane(bytePanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      contentScroll.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Contents"));
      JPanel command = new JPanel();
      JButton refreshButton = new JButton("Refresh");
      refreshButton.setFont(fontBold);
      refreshButton.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               synchronized(syncObj)
               {
                  bytePanel.clearBytes();
                  cachedReadBytes = null;
                  cachedReadBank = null;
               }
               enqueueRunTask(readMemoryTask);
            }
         }
      );
      command.add(refreshButton);
      JButton commitButton = new JButton("Commit Changes");
      commitButton.setFont(fontBold);
      commitButton.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               if(bytePanel.hasChanged())
               {
                  byte[] cachedRead = null, newBytes = null;
                  MemoryBank mb = null;
                  synchronized(syncObj)
                  {
                     cachedRead = cachedReadBytes;
                     mb = cachedReadBank;
                     try
                     {
                        newBytes = bytePanel.getBytes(mb.getSize());
                     }
                     catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                     {
                        setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                     }
                     if(newBytes==null || cachedRead==null ||
                           newBytes.length!=cachedRead.length)
                     {
                        bytePanel.clearBytes();
                        cachedReadBank = null;
                        cachedReadBytes = null;
                        enqueueRunTask(readMemoryTask);
                     }
                     else
                     {
                        enqueueRunTask(new WriteMemoryTask(adapter, container,
                                       mb, cachedRead, newBytes));
                     }
                  }
               }
            }
         }
      );
      command.add(commitButton);
      JPanel contentPanel = new JPanel(new BorderLayout(1,1));
      contentPanel.add(contentScroll, BorderLayout.CENTER);
      contentPanel.add(new JScrollPane(command), BorderLayout.SOUTH);

      add(featureScroll, BorderLayout.WEST);
      add(contentPanel, BorderLayout.CENTER);
      add(bankScroll, BorderLayout.NORTH);

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
      if(owc!=null)
      {
         Enumeration e = owc.getMemoryBanks();
         if(e!=null)
            return e.hasMoreElements();
         else
            return false;
      }
      else
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
            this.container = owc;
            this.romID = owc.getAddressAsString();
            this.memoryBanks = new Vector();
         }
         setStatus(Viewer.MESSAGE, "Setting up viewer...");
         Enumeration e = container.getMemoryBanks();
         while(e.hasMoreElements())
         {
            MemoryBank mb = (MemoryBank)e.nextElement();
            if(mb instanceof PagedMemoryBank)
            {
               PagedMemoryBank pmb = (PagedMemoryBank)mb;
            }
            if(mb instanceof OTPMemoryBank)
            {
               OTPMemoryBank otpmb = (OTPMemoryBank)mb;
            }
            memoryBanks.addElement(mb);
            bankListModel.addElement(mb.getBankDescription());
         }
         setStatus(Viewer.MESSAGE, "Done Setting up viewer.");
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
    * Clears the reference to the device container.
    */
   public void clearContainer()
   {
      synchronized(syncObj)
      {
         this.adapter = null;
         this.container = null;
         this.romID = null;
         this.memoryBanks = null;
         this.cachedReadBytes = null;
      }
      for(int i=0; i<TOTAL_FEATURES; i++)
         lblFeature[i].setText("");
      bankListModel.removeAllElements();
      bytePanel.clearBytes();
      lastIndex = -1;
      validate();
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
      while(executeRunTask())
        /* no-op */;
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
      MemoryViewer mv = new MemoryViewer();
      mv.setContainer(this.container);
      return mv;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for formatting, creating files and directories, and
   //                updating the directory and file information
   // -------------------------------------------------------------------------
   /**
    * ReadFeaturesTask encapsulates the action of reading the features of
    * a 1-Wire Device Memory Bank.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadFeaturesTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer l_container = null;
         Vector l_memoryBanks = null;
         synchronized(syncObj)
         {
            l_adapter = adapter;
            l_container = container;
            l_memoryBanks = memoryBanks;
         }

         if(l_adapter==null)
            return;

         int index = bankList.getSelectedIndex();
         if(index<0 || index>=l_memoryBanks.size())
            return;

         MemoryBank mb = (MemoryBank)l_memoryBanks.get(index);

         lblFeature[START].setText(" "
             +Long.toHexString(mb.getStartPhysicalAddress())+"H");
         lblFeature[SIZE].setText(" "+mb.getSize() + " bytes");
         lblFeature[IS_GP].setText(" "+mb.isGeneralPurposeMemory());
         lblFeature[IS_NV].setText(" "+mb.isNonVolatile());
         lblFeature[IS_RO].setText(" "+mb.isReadOnly());
         lblFeature[IS_RW].setText(" "+mb.isReadWrite());
         lblFeature[IS_OTP].setText(" "+mb.isWriteOnce());
      }
   }

   /**
    * ReadMemoryTask encapsulates the action of reading the entire contents of
    * a 1-Wire Device Memory Bank.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class ReadMemoryTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer l_container = null;
         Vector l_memoryBanks = null;
         synchronized(syncObj)
         {
            l_adapter = adapter;
            l_container = container;
            l_memoryBanks = memoryBanks;
         }

         if(l_adapter==null)
            return;

         int index = bankList.getSelectedIndex();
         if(index<0 || index>=l_memoryBanks.size())
            return;

         MemoryBank mb = (MemoryBank)l_memoryBanks.get(index);
         int size = mb.getSize();

         if(size>readBuf.length)
            readBuf = new byte[size];

         byte[][] extraInfo = null;
         byte[] cachedRead = null;
         try
         {
            setStatus(Viewer.MESSAGE, "Reading memory...");
            l_adapter.beginExclusive(true);
            //l_container.doSpeed();
            if(mb instanceof PagedMemoryBank)
            {
               PagedMemoryBank pmb = (PagedMemoryBank)mb;
               int len = pmb.getPageLength();
               int numPgs = (size/len) + (size%len>0?1:0);
               boolean hasExtra = pmb.hasExtraInfo();
               int extraSize = pmb.getExtraInfoLength();
               if(hasExtra)
                  extraInfo = new byte[numPgs][extraSize];
               int retryCnt = 0;
               for(int i=0; i<numPgs;)
               {
                  try
                  {
                     boolean readContinue = (i>0) && (retryCnt==0);
                     if(hasExtra)
                        pmb.readPage(i, readContinue, readBuf, i*len, extraInfo[i]);
                     else
                        pmb.readPage(i, readContinue, readBuf, i*len);
                     i++;
                     retryCnt = 0;
                  }
                  catch(Exception e)
                  {
                     if(++retryCnt>15)
                        throw e;
                  }
               }
            }
            else
            {
               int retryCnt = 0;
               while(true)
               {
                  try
                  {
                     mb.read(0,false,readBuf,0,size);
                     break;
                  }
                  catch(Exception e)
                  {
                     if(++retryCnt>15)
                        throw e;
                  }
               }
            }
         }
         catch(Exception e)
         {
            e.printStackTrace();
            setStatus(ERRMSG, e.toString());
            return;
         }
         finally
         {
            l_adapter.endExclusive();
         }
         setStatus(Viewer.MESSAGE, "Done Reading memory...");

         boolean doSet = true;
         if(cachedReadBytes!=null && cachedReadBytes.length==size)
         {
            doSet = false;
            for(int i=0; !doSet && i<size; i++)
               doSet = (cachedReadBytes[i]!=readBuf[i]);
         }

         if(doSet)
         {
            cachedRead = new byte[size];
            System.arraycopy(readBuf,0,cachedRead,0,size);
            setStatus(Viewer.MESSAGE, "Updating viewer contents...");
            if(mb instanceof PagedMemoryBank)
            {
               PagedMemoryBank pmb = (PagedMemoryBank)mb;
               int len = pmb.getPageLength();
               int numPgs = (size/len) + (size%len>0?1:0);
               String[] labels = new String[numPgs];
               boolean hasExtra = pmb.hasExtraInfo();
               int extraSize = pmb.getExtraInfoLength();
               int startAddress = mb.getStartPhysicalAddress();
               for(int i=0; i<numPgs; i++)
               {
                  StringBuffer sb = new StringBuffer();
                  sb.append("Page ");
                  sb.append(i);
                  sb.append(" (");
                  sb.append(Long.toHexString(startAddress+i*len).toUpperCase());
                  sb.append("H)");
                  if(hasExtra)
                  {
                     sb.append(" [");;
                     if(pmb.getExtraInfoDescription()!=null)
                        sb.append(pmb.getExtraInfoDescription());
                     sb.append(" ");
                     sb.append(Convert.toHexString(extraInfo[i], 0, extraSize, " "));
                     sb.append("] ");
                  }
                  labels[i] = sb.toString();
               }
               synchronized(syncObj)
               {
                  if(size<16)
                     bytePanel.setBytes(labels, 2, size, readBuf, 0, size);
                  else if(len<16)
                     bytePanel.setBytes(labels, 2, len, readBuf, 0, size);
                  else
                     bytePanel.setBytes(labels, 2, len/2, readBuf, 0, size);
                  cachedReadBytes = cachedRead;
                  cachedReadBank = mb;
               }
            }
            else
            {
               synchronized(syncObj)
               {
                  bytePanel.setBytes(readBuf,0,size);
                  cachedReadBytes = cachedRead;
                  cachedReadBank = mb;
               }
            }
            setStatus(Viewer.MESSAGE, "Done updating viewer contents.");
         }
      }
   }
   /**
    * WriteMemoryTask encapsulates the action of writing the entire contents of
    * a 1-Wire Device Memory Bank.
    */
   protected class WriteMemoryTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer task_container = null;
      MemoryBank task_memoryBank = null;
      byte[] task_cache = null;
      byte[] task_newBytes = null;

      public WriteMemoryTask(DSPortAdapter l_adapter,
                             OneWireContainer l_container,
                             MemoryBank l_mb,
                             byte[] l_cache, byte[] l_newBytes)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_memoryBank = l_mb;
         task_cache = l_cache;
         task_newBytes = l_newBytes;
      }

      public void executeTask()
      {
         setStatus(Viewer.MESSAGE, "Committing changes...");
         try
         {
            task_adapter.beginExclusive(true);
            task_container.doSpeed();
            if(task_memoryBank instanceof com.dalsemi.onewire.container.MemoryBankScratchSHAEE)
            {
               task_memoryBank.write(0, task_newBytes, 0, 8);
            }
            else if(!task_memoryBank.isNonVolatile())
            {
               task_memoryBank.write(0, task_newBytes, 0, task_newBytes.length);
            }
            else
            {
               int beginIndex = -1, endIndex = -1;
               for(int i=0; i<=task_newBytes.length; i++)
               {
                  if(i<task_newBytes.length && task_newBytes[i]!=task_cache[i])
                  {
                     if(beginIndex==-1)
                        beginIndex = endIndex = i;
                     else
                        endIndex = i;
                  }
                  else if(beginIndex!=-1)
                  {
                     setStatus(Viewer.VERBOSE,
                                "Writing data from: " + beginIndex
                                + " to: " + endIndex);
                     task_memoryBank.write(beginIndex, task_newBytes, beginIndex,
                                          endIndex-beginIndex+1);
                     System.arraycopy(task_newBytes, beginIndex,
                                      task_cache, beginIndex,
                                      endIndex-beginIndex+1);
                     beginIndex = endIndex = -1;
                  }
               }
            }
         }
         catch(Exception e)
         {
            e.printStackTrace();
            setStatus(ERRMSG, e.toString());
            return;
         }
         finally
         {
            task_adapter.endExclusive();
         }
         setStatus(Viewer.MESSAGE, "Done committing changes...");
      }
   }
}
