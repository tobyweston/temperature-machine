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
import javax.swing.tree.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.OneWireIOException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.file.OWFileInputStream;
import com.dalsemi.onewire.application.file.OWFileOutputStream;
import com.dalsemi.onewire.application.file.OWFile;
import com.dalsemi.onewire.application.monitor.DeviceMonitor;
import com.dalsemi.onewire.container.MemoryBank;
import com.dalsemi.onewire.container.PagedMemoryBank;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.tag.TaggedDevice;

/**
 * A 1-Wire Device File Viewer for integration in the OneWireViewer.
 * All devices that contain general-purpose memory are supported by
 * this viewer.  This viewer creates a tree view of the current file
 * system.  It allows creating, editing, and resizing files.  The
 * device can be formatted and new directories can be created.  Multi-
 * device filesystems are also supported.
 *
 * @author SH
 * @version 1.00
 */
public class FileViewer extends Viewer
   implements Runnable, ActionListener
{
   /* string constants */
   private static final String strTitle = "FileViewer";
   private static final String strTab = "File";
   private static final String strTip = "Displays raw File contents";

   /* container variables */
   private OneWireContainer container  = null;
   private TaggedDevice taggedDevice = null;

   /* visual components */
   private JTree tree = null;
   private DefaultTreeModel treeModel = null;
   private JTabbedPane tabbedPane = null;
   private BytePanel bytePanel = null;
   private JTextArea asciiText = null;
   private JScrollPane byteScroll = null, asciiScroll = null;

   /* Since ReadDirTask has no parameters, only a single instance of this
      task is needed */
   private final ViewerTask readDirTask = new ReadDirTask();
   /* Since ReadFileTask has no parameters, only a single instance of this
      task is needed */
   private final ViewerTask readFileTask = new ReadFileTask();

   private byte[] readBuf = new byte[32];
   private OWFile lastFileRead = null;

   /* indices for all action buttons */
   private static final int BUTTON_COUNT = 8;
   private static final int READ_DIR = 0;
   private static final int READ_FILE = 1;
   private static final int WRITE_FILE = 2;
   private static final int RESIZE_FILE = 3;
   private static final int DELETE = 4;
   private static final int FORMAT = 5;
   private static final int NEW_DIR = 6;
   private static final int NEW_FILE = 7;
   private static final String[] strButtonText = new String[]
   { "Read Dir", "Read File", "Write File", "Resize File", "Delete Selected",
     "Format Device", "Create New Directory", "Create New File" };

   /* an array of buttons that represent the buttons for each tasks */
   private JButton[] buttonList = null;

   /**
    * Constructs a new instance of FileViewer.
    */
   public FileViewer()
   {
      super(strTitle);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 3;

      // initialize the buttons
      this.buttonList = new JButton[BUTTON_COUNT];
      for(int i=0; i<BUTTON_COUNT; i++)
      {
         this.buttonList[i] = new JButton(strButtonText[i]);
         this.buttonList[i].setFont(Viewer.fontBold);
         this.buttonList[i].addActionListener(this);
      }

      // file tree
      JPanel treePanel = new JPanel();
      treePanel.setLayout(new BoxLayout(treePanel, BoxLayout.Y_AXIS));
      FileNode node = new FileNode();
      treeModel = new DefaultTreeModel(node);
      tree = new JTree(treeModel);
      tree.setShowsRootHandles(true);
      JScrollPane treeScroll = new JScrollPane(tree,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      treePanel.add(treeScroll);
      treePanel.add(buttonList[READ_DIR]);

      tabbedPane = new JTabbedPane();
      // hexadecimal display
      bytePanel = new BytePanel();
      byteScroll = new JScrollPane(bytePanel,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tabbedPane.addTab("Hex", byteScroll);

      // ascii display
      asciiText = new JTextArea();
      asciiScroll = new JScrollPane(asciiText,
                                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      tabbedPane.addTab("Ascii", asciiScroll);

      // command Panel beneath ascii and hex display
      JPanel command = new JPanel();
      command.add(buttonList[READ_FILE]);
      command.add(buttonList[WRITE_FILE]);
      command.add(buttonList[RESIZE_FILE]);
      JPanel contentPanel = new JPanel(new BorderLayout(1,1));
      contentPanel.add(tabbedPane, BorderLayout.CENTER);
      contentPanel.add(command, BorderLayout.SOUTH);

      // File command panel
      JPanel commandPanel = new JPanel();
      commandPanel.setPreferredSize(new Dimension(540,80));
      JScrollPane commandScroll = new JScrollPane(commandPanel,
                                   JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                                   JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      commandPanel.setLayout(new BoxLayout(commandPanel,BoxLayout.X_AXIS));
      commandPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "File Commands"));
      commandPanel.add(buttonList[FORMAT]);
      commandPanel.add(buttonList[NEW_DIR]);
      commandPanel.add(buttonList[NEW_FILE]);
      commandPanel.add(buttonList[DELETE]);

      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                         treePanel, contentPanel);
      splitPane.setDividerLocation(180);

      add(splitPane, BorderLayout.CENTER);
      add(commandScroll,BorderLayout.NORTH);

      clearContainer();
    }

   /**
    * Action handler for all button activity.  Disables all buttons until
    * activity is complete.
    * @param ae The event describing the action
    */
   public void actionPerformed(ActionEvent ae)
   {
      // disable all buttons
      for(int i=0; i<buttonList.length; i++)
      {
         buttonList[i].setEnabled(false);
      }

      Object button = ae.getSource();
      if(button==buttonList[READ_DIR])
      {
         enqueueRunTask(readDirTask);
      }
      else if(button==buttonList[READ_FILE])
      {
         enqueueRunTask(readFileTask);
      }
      else if(button==buttonList[WRITE_FILE])
      {
         if(lastFileRead!=null)
         {
            try
            {
               byte[] bytes = null;
               Object o = tabbedPane.getSelectedComponent();
               if(o==asciiScroll)
                  bytes = asciiText.getText().getBytes();
               else
                  bytes = bytePanel.getBytes(-1);
   
               ViewerTask vt = null;
               synchronized(syncObj)
               {
                  vt = new WriteFileTask(adapter, container,
                                       lastFileRead.getAbsolutePath(),
                                       bytes);
               }
               enqueueRunTask(vt);
            }
            catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
            {
               setStatus(ERRMSG, "Error during hex string conversion: " + ce);
            }
         }
         else
         {
            setStatus(ERRMSG, "You must select a file and read it first");
         }
      }
      else if(button==buttonList[RESIZE_FILE])
      {
         if(lastFileRead!=null)
         {
            String newSize = JOptionPane.showInputDialog(FileViewer.this,
               new JLabel("New Size of OWFile " + lastFileRead.getName()
                  + " (" + lastFileRead.length() + " bytes)" ),
               "Size of File", JOptionPane.QUESTION_MESSAGE);
            try
            {
               int i = Integer.parseInt(newSize);

               ViewerTask vt = null;
               synchronized(syncObj)
               {
                  vt = new ResizeFileTask(adapter, container,
                                       lastFileRead.getAbsolutePath(),
                                       bytePanel.getBytes(-1), i);
               }
               enqueueRunTask(vt);
            }
            catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
            {
               setStatus(ERRMSG, "Error during hex string conversion: " + ce);
            }
            catch(NumberFormatException nfe)
            {
               setStatus(ERRMSG, "Bad Number Format");
            }
         }
         else
         {
            setStatus(ERRMSG, "You must select a file and read it first");
         }
      }
      else if(button==buttonList[FORMAT])
      {
         DSPortAdapter l_adapter = FileViewer.this.adapter;

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(new Label("Select Device(s) for Format"),
                   BorderLayout.NORTH);

         DeviceMonitor monitor = new DeviceMonitor(l_adapter);
         Vector arrivals = new Vector();
         try
         {
            // should be no departures on first search
            monitor.search(arrivals, null);
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Exception while searching: " + e.getMessage());
         }

         Object[] addresses = arrivals.toArray();
         arrivals.setSize(0);
         for(int i=0; i<addresses.length; i++)
         {
            OneWireContainer c = DeviceMonitor.getDeviceContainer(adapter,
                                                            (Long)addresses[i]);
            if(containerSupported(c))
               arrivals.addElement(c);
         }

         JList list = new JList(arrivals);
         list.setFont(Viewer.fontPlain);
         JScrollPane listPane = new JScrollPane(list);
         panel.add(listPane, BorderLayout.CENTER);

         JOptionPane.showMessageDialog(FileViewer.this, panel);

         Object[] oArray = list.getSelectedValues();
         OneWireContainer[] owc = new OneWireContainer[oArray.length];
         for(int i=0; i<owc.length; i++)
            owc[i] = (OneWireContainer)oArray[i];


         if(owc.length>0)
         {
            enqueueRunTask(new FormatTask(l_adapter, owc));
         }
         else
         {
            setStatus(ERRMSG,"No Devices Selected for Format");
         }
      }
      else if(button==buttonList[NEW_DIR])
      {
         String newDir = JOptionPane.showInputDialog(FileViewer.this,
               new JLabel("Enter a name for the directory (4 chars)"),
               "Name of Directory", JOptionPane.QUESTION_MESSAGE);

         DSPortAdapter l_adapter = null;
         OWFile f = null;
         FileNode node = null;
         synchronized(syncObj)
         {
            l_adapter = FileViewer.this.adapter;
            TreePath path = tree.getSelectionPath();

            if(path!=null)
               node = (FileNode)path.getLastPathComponent();
            else
               node = (FileNode)treeModel.getRoot();

            f = node.getFile();
         }
         if(f==null)
         {
            setStatus(ERRMSG,"No root filesystem");
            return;
         }

         if(f.isFile())
         {
            f = f.getParentFile();
            node = (FileNode)node.getParent();
         }
         enqueueRunTask(new NewDirTask(l_adapter, f, newDir));
      }
      else if(button==buttonList[NEW_FILE])
      {
         String newFile = JOptionPane.showInputDialog(FileViewer.this,
               new JLabel("Enter a name for the file (4 chars + ext)"),
               "Name of File", JOptionPane.QUESTION_MESSAGE);

         DSPortAdapter l_adapter = null;
         OWFile f = null;
         FileNode node = null;
         synchronized(syncObj)
         {
            l_adapter = FileViewer.this.adapter;
            TreePath path = tree.getSelectionPath();

            if(path!=null)
               node = (FileNode)path.getLastPathComponent();
            else
               node = (FileNode)treeModel.getRoot();

            f = node.getFile();
         }

         if(f!=null)
         {
            if(f.isFile())
            {
               f = f.getParentFile();
               node = (FileNode)node.getParent();
            }
            enqueueRunTask(new NewFileTask(l_adapter, f, newFile));
         }
         else
         {
            setStatus(ERRMSG,"No root filesystem");
         }
      }
      else if(button==buttonList[DELETE])
      {
         DSPortAdapter l_adapter = null;
         OWFile f = null;
         synchronized(syncObj)
         {
            l_adapter = FileViewer.this.adapter;
            TreePath path = tree.getSelectionPath();
            FileNode node = null;

            if(path!=null)
               node = (FileNode)path.getLastPathComponent();
            else
               node = (FileNode)treeModel.getRoot();

            f = node.getFile();
         }
         if(f!=null)
         {
            enqueueRunTask(new DeleteFileTask(l_adapter, f));
         }
         else
         {
            setStatus(ERRMSG,"No root filesystem");
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
      if (owc == null)
         return false;

      for (Enumeration bank_enum = owc.getMemoryBanks();
             bank_enum.hasMoreElements(); )
      {
         MemoryBank mb = (MemoryBank) bank_enum.nextElement();

         if (!mb.isGeneralPurposeMemory() || !mb.isNonVolatile() ||
             !(mb instanceof PagedMemoryBank))
            continue;

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
            this.container = owc;
            this.romID = owc.getAddressAsString();
         }
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
      }
      FileNode node = new FileNode();
      treeModel.setRoot(node);
      bytePanel.clearBytes();
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

      for(int i=0; i<buttonList.length; i++)
      {
         buttonList[i].setEnabled(true);
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
      FileViewer mv = new FileViewer();
      mv.setContainer(this.container);
      return mv;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for formatting, creating files and directories, and
   //                updating the directory and file information
   // -------------------------------------------------------------------------
   /**
    * FormatTask encapsulates the action of formatting a device or multiple
    * devices.
    */
   protected class FormatTask extends ViewerTask
   {
      private OneWireContainer[] task_containers  = null;
      private DSPortAdapter task_adapter  = null;

      public FormatTask(DSPortAdapter adapter, OneWireContainer[] containers)
      {
         this.task_adapter = adapter;
         this.task_containers = containers;
      }

      public void executeTask()
      {
         try
         {
            setStatus(MESSAGE, "Formatting Device(s)...");
            task_adapter.beginExclusive(true);
            OWFile f = new OWFile(task_containers, "/");
            f.format();
            f.close();
            setStatus(MESSAGE, "Done Formatting Device(s).");
            enqueueRunTask(readDirTask);
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Exception while formatting: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * NewDirTask encapsulates the action of creating a new directory entry
    * on a 1-Wire filesystem.
    */
   protected class NewDirTask extends ViewerTask
   {
      private DSPortAdapter task_adapter  = null;
      private OWFile task_file = null;
      private String task_name = null;

      public NewDirTask(DSPortAdapter adapter, OWFile parentFile, String name)
      {
         this.task_adapter = adapter;
         this.task_file = parentFile;
         this.task_name = name;
      }

      public void executeTask()
      {
         try
         {
            setStatus(MESSAGE, "Creating Directory...");
            task_adapter.beginExclusive(true);
            OWFile newFile = new OWFile(task_file, task_name);
            if(newFile.mkdir())
            {
               newFile.close();
               enqueueRunTask(readDirTask);
               setStatus(MESSAGE, "Done Creating Directory.");
            }
            else
            {
               newFile.close();
               setStatus(MESSAGE, "Failed to Create Directory.");
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Exception while creating directory: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * NewFileTask encapsulates the action of creating a new file entry
    * on a 1-Wire filesystem.
    */
   protected class NewFileTask extends ViewerTask
   {
      private DSPortAdapter task_adapter  = null;
      private OWFile task_file = null;
      private String task_name = null;

      public NewFileTask(DSPortAdapter adapter, OWFile parentFile, String name)
      {
         this.task_adapter = adapter;
         this.task_file = parentFile;
         this.task_name = name;
      }

      public void executeTask()
      {
         try
         {
            setStatus(MESSAGE, "Creating New File...");
            task_adapter.beginExclusive(true);
            OWFile newFile = new OWFile(task_file, task_name);
            if(newFile.createNewFile())
            {
               newFile.close();
               enqueueRunTask(readDirTask);
               setStatus(MESSAGE, "Done Creating New File.");
            }
            else
            {
               newFile.close();
               setStatus(MESSAGE, "Failed to Create New File.");
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Exception while creating directory: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * NewFileTask encapsulates the action of deleting a file entry
    * from a 1-Wire filesystem.
    */
   protected class DeleteFileTask extends ViewerTask
   {
      private DSPortAdapter task_adapter  = null;
      private OWFile task_file = null;

      public DeleteFileTask(DSPortAdapter adapter, OWFile file)
      {
         this.task_adapter = adapter;
         this.task_file = file;
      }

      public void executeTask()
      {
         try
         {
            setStatus(MESSAGE, "Deleting File...");
            task_adapter.beginExclusive(true);
            if(task_file.delete())
            {
               task_file.close();
               enqueueRunTask(readDirTask);
               setStatus(MESSAGE, "Done Deleting File.");
            }
            else
            {
               setStatus(MESSAGE, "Failed to Delete File.");
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Exception while creating directory: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * ReadDirTask encapsulates the action of reading the entire directory of
    * a 1-Wire filesystem for display in a JTree.  Since this class is,
    * essentially, parameter-less (or, rather it grabs its parameters from
    * the current state of the viewer) only one instance is really necessary.
    */
   protected class ReadDirTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null||container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         try
         {
            setStatus(Viewer.MESSAGE, "Reading Directory...");
            l_adapter.beginExclusive(true);
            OWFile f = new OWFile(l_container, "/");
            if(f.isDirectory())
            {
               FileNode node = (FileNode)treeModel.getRoot();
               node.setUserObject(f);
               node.removeAllChildren();
               FileNode selectPath = createTreeNodes(node, f.listFiles());
               treeModel.reload();
               if(selectPath!=null)
               {
                  TreeNode[] nodes = treeModel.getPathToRoot(selectPath);
                  tree.setSelectionPath(new TreePath(nodes));
               }
               setStatus(Viewer.MESSAGE, "Done Reading Directory.");
            }
            else
            {
               setStatus(Viewer.MESSAGE, "No FileSystem");
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, e.toString());
            e.printStackTrace();
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
      private FileNode createTreeNodes(FileNode node, OWFile[] files)
         throws OneWireException, OneWireIOException
      {
         if(files==null || files.length==0)
            return null;
         FileNode selectPath = null, temp = null;
         for(int i=0; i<files.length; i++)
         {
            FileNode newNode = new FileNode(files[i]);
            node.add(newNode);
            if(files[i].equals(lastFileRead))
               selectPath = newNode;
            if(files[i].isDirectory())
            {
               node.setAllowsChildren(true);
               temp = createTreeNodes(newNode, files[i].listFiles());
               if(temp!=null)
                  selectPath = temp;
            }
         }
         return selectPath;
      }
   }

   /**
    * ReadFileTask encapsulates the action of reading the entire contents of
    * a 1-Wire file.  Since this class is, essentially, parameter-less (or,
    * rather it grabs its parameters from the current state of the viewer)
    * only one instance is really necessary.
    */
   protected class ReadFileTask extends ViewerTask
   {
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null||container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         try
         {
            bytePanel.clearBytes();

            TreePath path = tree.getSelectionPath();
            if(path==null)
               return;

            setStatus(MESSAGE, "Reading File...");
            l_adapter.beginExclusive(true);
            OWFile f = ((FileNode)path.getLastPathComponent()).getFile();
            if(f==null)
               return;

            if(!f.isFile())
            {
               setStatus(MESSAGE, "Not a file.");
            }
            else if(!f.canRead())
            {
               setStatus(MESSAGE, "File Not Readable");
            }
            else
            {
               int length = (int)f.length();
               byte[] readBuf = new byte[length];
               OWFileInputStream fis = new OWFileInputStream(l_container,
                                               f.getAbsolutePath());
               length = fis.read(readBuf,0,length);
               if(length<0)
                  length = (int)f.length();
               fis.close();
               boolean isAscii = true;
               bytePanel.setBytes(readBuf,0,length);
               for(int i=0; i<length; i++)
               {
                  if(readBuf[i]==0)
                  {
                     isAscii = false;
                     readBuf[i]=0x20;
                  }
               }
               asciiText.setText(new String(readBuf,0,length));
               if(isAscii)
                  tabbedPane.setSelectedIndex(1);
               else
                  tabbedPane.setSelectedIndex(0);
               setStatus(MESSAGE, "Done Reading File.");
               synchronized(syncObj)
               {
                  lastFileRead = f;
               }
            }

         }
         catch(Exception e)
         {
            setStatus(ERRMSG, e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
      }
   }

   /**
    * WriteFileTask encapsulates the action of writing the entire contents of
    * a 1-Wire file.
    */
   protected class WriteFileTask extends ViewerTask
   {
      private DSPortAdapter task_adapter = null;
      private OneWireContainer task_container = null;
      private String task_filename = null;
      private byte[] task_bytes = null;

      public WriteFileTask(DSPortAdapter l_adapter, OneWireContainer l_container,
                            String filename, byte[] l_bytes)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_filename = filename;
         task_bytes = l_bytes;
      }

      public void executeTask()
      {
         try
         {
            setStatus(MESSAGE, "Writing File: Name=" + task_filename + " Size="+ task_bytes.length);
            task_adapter.beginExclusive(true);
            OWFileOutputStream fos = new OWFileOutputStream(task_container,
                                                task_filename, false);
            fos.write(task_bytes, 0, (int)task_bytes.length);
            fos.close();
            enqueueRunTask(readDirTask);
            enqueueRunTask(readFileTask);
            setStatus(MESSAGE, "Done Writing File.");
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, e.toString());
            e.printStackTrace();
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * ResizeFileTask encapsulates the action of changing the size of
    * a 1-Wire file.
    */
   protected class ResizeFileTask extends ViewerTask
   {
      private DSPortAdapter task_adapter = null;
      private OneWireContainer task_container = null;
      private String task_filename = null;
      private byte[] task_bytes = null;
      private int task_newSize = 0;

      public ResizeFileTask(DSPortAdapter l_adapter, OneWireContainer l_container,
                            String filename, byte[] l_bytes, int newSize)
      {
         task_adapter = l_adapter;
         task_container = l_container;
         task_filename = filename;
         task_bytes = l_bytes;
         task_newSize = newSize;
      }

      public void executeTask()
      {
         try
         {
            task_adapter.beginExclusive(true);
            OWFileOutputStream fos = new OWFileOutputStream(task_container,
                                              task_filename, false);
            fos.write(task_bytes, 0, Math.min(task_newSize,task_bytes.length));
            if(task_newSize>task_bytes.length)
            {
               int diff = task_newSize-task_bytes.length;
               for(int i=0; i<diff; i++)
                  fos.write(0);
            }
            fos.close();
            enqueueRunTask(readDirTask);
            enqueueRunTask(readFileTask);
            setStatus(MESSAGE, "Done Resizing File.");
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }
}

/**
 * An extension of DefaultMutableTreeNode for placing OWFiles
 * into a Swing JTree.
 */
class FileNode extends DefaultMutableTreeNode
{
   /** the file object this node holds */
   private OWFile file = null;
   /** cache the toString */
   private String toString;

   /**
    * Constructs a new file node with the provided OWFile
    * object.
    * @param f the OWFile this file node will hold.
    */
   public FileNode(OWFile f)
   {
      super(f, f.isDirectory());
      this.file = f;
      if(f==null || f.getName()==null)
         toString = "/";
      else
         toString = f.getName() + "  (" + f.length() + " bytes)";
   }

   /**
    * Constructs a new file node with no OWFile object.
    */
   public FileNode()
   {
      super("N/A", true);
      toString = "N/A";
   }

   /**
    * returns true if this file node has no children.
    * @return true if this file node has no children.
    */
   public boolean isLeaf()
   {
      return (file!=null && file.isFile());
   }

   /**
    * overrides default getChildAt for null
    * check.
    */
   public TreeNode getChildAt(int index)
   {
      if(super.children==null)
         return null;
      return super.getChildAt(index);
   }


   /**
    * Returns the OWFile this file node holds.
    * @return the OWFile this file node holds.
    */
   public OWFile getFile()
   {
      return file;
   }

   /**
    * overrides default setUserObject for caching
    * certain values.
    * @param f the OWFile this file node will hold.
    */
   public void setUserObject(OWFile f)
   {
      super.setUserObject(f);
      this.file = f;
      if(f==null || f.getName()==null)
         toString = "/";
      else
         toString = f.getName() + "  (" + f.length() + " bytes)";
   }

   /**
    * Returns the name of the file and it's length.
    *
    * @return the name of the file and it's length.
    */
   public String toString()
   {
      return toString;
   }

   /**
    * If the provided tree node is a child of this node,
    * its index is returned.
    *
    * @param aChild the child to search for
    * @return the index of the child
    */
   public int getIndex(TreeNode aChild)
   {
      if (aChild == null || children==null)
         return -1;
      return children.indexOf(aChild);	// linear search
   }

   /**
    * Checks to see if two file nodes are equivalent
    *
    * @param o the object to test for equivalence
    * @return true if the object is a file node
    *         holding the same file.
    */
   public boolean equals(Object o)
   {
      if(!(o instanceof FileNode))
         return false;

      FileNode n = (FileNode)o;

      if(n.file==null)
         return this.file==null;
      
      try
      {
         if(n.file.getFD()==null || this.file.getFD()==null)
            return this.file.getFD()==n.file.getFD();
      }
      catch(java.io.IOException ioe)
      {
         return false;
      }

      return n.file.equals(this.file);
   }
}
