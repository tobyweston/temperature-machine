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


import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.JPanel;
import javax.swing.JLabel;
import java.util.List;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.debug.Debug;
import com.dalsemi.onewire.application.tag.TaggedDevice;

/**
 * Abstract superclass exposing all functionality for the supported viewers
 * in the OneWireViewer application.  All viewers (i.e. FileViewer, MemoryViewer,
 * TemperatureViewer) extend the Viewer abstract class.  The Viewer class
 * enforces BorderLayout on the root panel of all Viewers.  This is so the
 * Viewer can provide a common status label, to appear in the SOUTH region,
 * for passing informative messages about Viewer Events.
 */
public abstract class Viewer extends JPanel
{
   // -------------------------------
   //  Constants
   // -------------------------------

   //Cached constants for MessageLog message levels
   protected static final int ERRMSG = MessageLog.ERRMSG;
   protected static final int WARNMSG = MessageLog.WARNMSG;
   protected static final int MESSAGE = MessageLog.MESSAGE;
   protected static final int VERBOSE = MessageLog.VERBOSE;

   //debug booleans, for timing of the poll and run threads
   private static final boolean TIME_POLL = false, TIME_RUN = false;
   private static long runTimeVar = 0;
   private static long pollTimeVar = 0;

   // standard font for all viewers
   public static final Font fontPlain = new Font("DialogInput", Font.PLAIN, 12);
   public static final Font fontBold = new Font("DialogInput", Font.BOLD, 12);
   public static final Font fontLarge = new Font("DialogInput", Font.PLAIN, 14);
   public static final Font fontLargeBold = new Font("DialogInput", Font.BOLD, 14);

   // -------------------------------
   //  Variables
   // -------------------------------

   /* the title of the viewer, or basically the name of the class */
   protected String viewerTitle = null;

   /* Object used for synchronized access to objects shared between threads */
   protected Object syncObj = new Object();

   /* Label for informative messages, located in SOUTH region of Viewer */
   protected JLabel status = new JLabel(" ");

   /* Log file for holding (and possibly displaying) all messages */
   protected MessageLog log = null;

   /* The adapter in use for the current device this viewer is displaying
      information for */
   protected DSPortAdapter adapter = null;

   /* The cached 64-bit Rom ID (or network address) for the current device this
      viewer is displaying information for */
   protected String romID = null;

   /* An ordered list of ViewerTasks that will be executed in the run method
      of the current viewer */
   protected List runList = null;

   /* An ordered list of ViewerTasks that will be executed in the poll method
      of the current viewer */
   protected List pollList = null;

   protected int majorVersionNumber = 1;
   protected int minorVersionNumber = 0;

   /**
    * Don't use default constructor!
    */
   private Viewer()
   {;}

   /**
    * Sets the layout to BorderLayout, sets the title appropriately, adds
    * status label to SOUTH, initializes the run and poll ViewerTask queues.
    *
    * @param title The title of the viewer (usually the same as class name)
    */
   public Viewer(String title)
   {
      super(new BorderLayout());
      this.viewerTitle = title;
      add(status, BorderLayout.SOUTH);
      runList = new java.util.ArrayList();
      pollList = new java.util.ArrayList();
   }

   /**
    * Returns true if this viewer is allowed to be spawned in a new window.
    *
    * @return true if this viewer is allowed to be spawned in a new window.
    */
   public boolean isCloneable()
   {
      return true;
   }

   /**
    * Sets the current logger, the object where all messages are passed
    * for either display to the user or saved to a file.
    *
    * @param log MessageLog for holding all messages
    */
   public final void setLogger(MessageLog log)
   {
      this.log = log;
   }

   /**
    * Adds a message to the message log and sets the message as the text
    * for the status label.
    *
    * @param msgType The type of message (ERRMSG, WARNING, MESSAGE, VERBOSE)
    * @param txt The text of the message
    */
   protected final void setStatus(int msgType, String txt)
   {
      synchronized (syncObj)
      {
         if (log != null)
            log.addEntry(msgType, viewerTitle, romID, txt);
         status.setText(" " + txt);
      }
   }

   public int getMajorVersionNumber()
   {
      return this.majorVersionNumber;
   }

   public int getMinorVersionNumber()
   {
      return this.minorVersionNumber;
   }

   // ------------------------------------------------------------------------
   // Viewer Abstract Methods
   // ------------------------------------------------------------------------
   /**
    * Checks if this viewer supports the supplied container.
    *
    * @param owc - container to check for viewer support.
    *
    * @return 'true' if this viewer supports the provided
    *   container.
    */
   public abstract boolean containerSupported(OneWireContainer owc);

   /**
    * Checks if this viewer supports the supplied TaggedDevice.
    *
    * @param td - TaggedDevice to check for viewer support.
    *
    * @return 'true' if this viewer supports the provided
    *   TaggedDevice.
    */
   public abstract boolean containerSupported(TaggedDevice td);

   /**
    * Sets the container for this viewer.
    *
    * @param owc OneWireContainer of this viewer
    */
   public abstract void setContainer(OneWireContainer owc);

   /**
    * Sets the container by providing a TaggedDevice.
    *
    * @param td TaggedDevice representing this device
    */
   public abstract void setContainer(TaggedDevice td);

   /**
    * Clears the reference to the device container.
    */
   public abstract void clearContainer();

   /**
    * Gets the string that represents this Viewer's title
    *
    * @return viewer's title
    */
   public abstract String getViewerTitle();

   /**
    * Gets the string that represents this Viewer's description
    *
    * @return viewer's description
    */
   public abstract String getViewerDescription();

   /**
    * Create a complete clone of this viewer, including reference to
    * current container.  Used to display viewer in new window.
    *
    * @return a clone of the current viewer
    */
   public abstract Viewer cloneViewer();

   /**
    * Returns <code>true</code> if Viewer still has pending tasks it must
    * complete.
    *
    * @return <code>true</code> if Viewer still has pending tasks it must
    * complete.
    */
    public abstract boolean isBusy();

   // ------------------------------------------------------------------------
   // ViewerTask functionality
   // ------------------------------------------------------------------------
   protected abstract class ViewerTask
   {
      public abstract void executeTask();
   }

   protected final boolean hasRunTasks()
   {
      synchronized (runList)
      {
         return runList.size()>0;
      }
   }

   protected final boolean hasPollTasks()
   {
      synchronized (pollList)
      {
         return pollList.size()>0;
      }
   }

   protected final void enqueueRunTask(ViewerTask vt)
   {
//      System.out.println("enqueueRunTask: ViewerTask=" + vt.getClass().getName());
      enqueueTask(runList, vt);
   }

   protected void enqueuePollTask(ViewerTask vt)
   {
//      System.out.println("enqueuePollTask: ViewerTask=" + vt.getClass().getName());
      enqueueTask(pollList, vt);
   }

   private void enqueueTask(List list, ViewerTask vt)
   {
      if(vt==null)
         return;
      synchronized (list)
      {
         // check if task is already in the list
         int i = list.indexOf(vt);
         if(i<0)
            // add new task to the end of the list
            list.add(vt);
         else
            // bump task to the end of the list
            list.add(list.remove(i));
      }
   }

   protected final boolean executeRunTask()
   {
      long currTime;
      if(TIME_RUN)
      {
         currTime = System.currentTimeMillis();
         System.out.println("Milliseconds since last run: " + (currTime - runTimeVar));
         runTimeVar = currTime;
      }

//      System.out.println("executeTask: Run ");
      boolean bRetVal = executeTask(runList);
//      System.out.println("executeTask: Run Done, more=" + bRetVal);

      if(TIME_RUN)
      {
         currTime = System.currentTimeMillis();
         System.out.println("      required for this run: " + (currTime - runTimeVar));
         runTimeVar = currTime;
      }

      return bRetVal;
   }

   protected boolean executePollTask()
   {
      long currTime;
      if(TIME_POLL)
      {
         currTime = System.currentTimeMillis();
         System.out.println("Milliseconds since last poll: " + (currTime - pollTimeVar));
         pollTimeVar = currTime;
      }

//      System.out.println("executeTask: Poll, Viewer=" + this.getClass().getName());
      boolean bRetVal = executeTask(pollList);
//      System.out.println("executeTask: Poll Done, more=" + bRetVal);

      if(TIME_POLL)
      {
         currTime = System.currentTimeMillis();
         System.out.println("      required for this poll: " + (currTime - pollTimeVar));
         pollTimeVar = currTime;
      }

      return bRetVal;
   }

   private final boolean executeTask(List list)
   {
      // check for no-op
      if(list.size()==0)
      {
//         System.out.println("executeTask: NO TASKS");
         return false;
      }

      ViewerTask vt = null;
      synchronized (list)
      {
         // remove from the beginning of the queue
         vt = (ViewerTask)list.remove(0);
      }
//      System.out.println("executeTask: ViewerTask=" + vt.getClass().getName());
      // run the task
      vt.executeTask();

      // return whether or not there are any more left
      return (list.size()!=0);
   }
}