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
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import com.dalsemi.onewire.application.monitor.*;
import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.*;

/**
 * OneWireViewer - a graphical user application demonstrating most of
 * the APIs functionality.  The OneWireViewer is a frame with two panels,
 * one showing a list of all devices on the network and a second showing
 * a tabbed pane view of all the different ways of handling the device.
 * For example, if a DS1921 was selected from the device list, a tab would
 * be shown for it's clock interface, mission details, and current
 * temperature readings.  The OneWireViewer is easily extended to add
 * support for more "viewers" to expose additional functionality for each
 * part.  To add new viewer, simply extend the "Viewer" abstract class, then
 * add the name of your viewer to the list of supported viewers in the
 * onewireviewer.properties file.
 *
 *
 * @author SKH
 * @version 1.0
 */
public final class OneWireViewer extends JFrame
{
   /* ------------------------------------
    * Constants
    * ------------------------------------
    */

   /** String for title of frame */
   private static final String strTitle = "OneWireViewer";

   /** the version number for the OneWireViewer proper. */
   private static final int owvVersionNumber = 3;

   /* Properties file label constants and default values */
   public static final String OWV_VERSION = "OneWireViewer.Version";
   public static final String MESSAGE_LOG_LEVEL = "message.log.level";
   public static final int MESSAGE_LOG_LEVEL_DEF = MessageLog.MESSAGE;
   public static final String VIEWER_LOCATION_X = "viewer.location.X";
   public static final int VIEWER_LOCATION_X_DEF = 0;
   public static final String VIEWER_LOCATION_Y = "viewer.location.Y";
   public static final int VIEWER_LOCATION_Y_DEF = 0;
   public static final String VIEWER_SIZE_WIDTH = "viewer.size.width";
   public static final int VIEWER_SIZE_WIDTH_DEF = 800;
   public static final String VIEWER_SIZE_HEIGHT = "viewer.size.height";
   public static final int VIEWER_SIZE_HEIGHT_DEF = 600;
   public static final String POLLING_RATE_INDEX = "polling.rate.index";
   public static final String ADAPTER_NAME = "adapter.name";
   public static final String ADAPTER_PORT = "adapter.port";
   public static final String LOOK_AND_FEEL = "window.look.feel";
   public static final String LOOK_AND_FEEL_DEF = "javax.swing.plaf.metal.MetalLookAndFeel";
   public static final String XML_TAGGING_FILENAME = "XML.Tagging.Filename";
   public static final String ENABLE_TAG_SEARCHING = "XML.Tagging.Enabled";
   public static final String ENABLE_NORMAL_SEARCHING = "Normal.Searching.Enabled";
   public static final String SHOW_ALTERNATE_NAMES = "show.alternate.names";
   public static final String ADAPTER_SPEED = "adapter.speed";
   public static final String ALLOW_PURE_SERIAL_ADAPTER = "Pure.Serial.Adapter.Enabled";

   private static final int BUSY_TIMEOUT = 100;

   /* ------------------------------------
    * Member variables
    * ------------------------------------
    */

   /** text to be displayed in the about box */
   private String aboutString = "OneWireViewer  \n" +
      "Copyright 2001-2002 Maxim/Dallas Semiconductor  \n\n" +
      "Source of 1-Wire API and OneWireViewer \n" +
      "are available for download here: \n" +
      "ftp://ftp.dalsemi.com/pub/auto_id/public/OneWireAPIsrc.jar \n" +
      "ftp://ftp.dalsemi.com/pub/auto_id/public/OneWireViewersrc.jar \n\n";

   /** Device adapter used to search for devices */
   private DSPortAdapter adapter = null;


   /** a panel showing a list of all devices available on the 1-Wire net */
   private DevicePanel deviceList = null;
   /** Monitor for searching the 1-Wire network */
   private DeviceMonitor deviceMonitor = null;

   /** Tabbed pane shows all of the supported viewers */
   private JTabbedPane tabbedPane = null;

   /** Factory class for generating all of the supported viewers */
   private ViewerFactory viewerFactory = null;

   /** check box items for pausing/resuming device/tag monitor */
   private JCheckBox taggingModeEnabled, normalModeEnabled, pauseSearching;

   /** menu item for displaying alternate names of 1-Wire devices */
   JCheckBoxMenuItem alternateNamesMenuItem = null;

   /** buttons for selecting device polling rate */
   private JCheckBoxMenuItem[] pollRateButtons = null;
   /** button group for polling rate buttons */
   private ButtonGroup pollRateButtonGroup = null;

   /** Hashtable contains frames for all "windowed" viewers */
   private java.util.Hashtable frameHash = new java.util.Hashtable();
   /** Hashtable contains polling ThreadTimers for all "windowed" viewers */
   private java.util.Hashtable pollThreadHash = new java.util.Hashtable();
   /** Hashtable contains running ThreadTimers for all "windowed" viewers */
   private java.util.Hashtable runThreadHash = new java.util.Hashtable();

   /** Timer object which periodically calls the "poll" method on viewers */
   private ThreadTimer pollViewer = null;
   /** Timer object which periodically calls the "run" method on viewers */
   private ThreadTimer runViewer = null;

   /** for logging all error messages */
   private MessageLogViewer log = null;
   /** A frame for displaying all error messages */
   private JFrame logFrame = null;
   /** whether or not the message log is currently visible */
   private boolean logShown = false;

   /** viewer for controlling tagging properties */
   private TagMonitor tagMonitor = null;

   /** dialog used to display busy status of a viewer tab */
   private JDialog dlgBusy = null;
   /** text to indicate that a viewer tab is busy */
   private JLabel lblBusyText = null;

   /** used to set the speed of 1-Wire Device communication */
   private int deviceSpeed = DSPortAdapter.SPEED_REGULAR;

   /** indicates whether or not the default tag files were loaded or not */
   private boolean savedXMLTaggedFilesLoaded = false;

   /* ------------------------------------
    * Constructors
    * ------------------------------------
    */

   /**
    * Creates a new OneWireViewer frame.  Uses a split pane to show a
    * device list on the left hand side.  On the right hand side is a
    * tabbed pane, which will contain a list of the supported Viewers.
    *
    * @param adapter the DSPortAdapter from which the DevicePanel will be
    *        populated.
    *
    * @see DevicePanel
    * @see Viewer
    */
   public OneWireViewer(DSPortAdapter adapter)
   {
      super(strTitle);

      this.adapter = adapter;

      // load the image for the frame
      ClassLoader cl = this.getClass().getClassLoader();
      java.net.URL imgU = cl.getResource("images/OneWireViewerIcon.gif");
      if(imgU!=null)
         this.setIconImage(new ImageIcon(imgU).getImage());

      // create the message log for displaying in another frame
      this.log = new MessageLogViewer();
      this.log.setLevel(ViewerProperties.getPropertyInt(MESSAGE_LOG_LEVEL,
                                                        MESSAGE_LOG_LEVEL_DEF));

      // pre-load all registered viewers
      this.viewerFactory = new ViewerFactory(this.log);

      // build the version string
      int owvVersionMajor = 0;
      int owvVersionMinor = 0;
      String versionString = "";
      Enumeration enum = this.viewerFactory.getAllViewers();
      while(enum.hasMoreElements())
      {
         Viewer viewer = (Viewer)enum.nextElement();
         int versionMajor = viewer.getMajorVersionNumber();
         int versionMinor = viewer.getMinorVersionNumber();
         owvVersionMajor += versionMajor;
         owvVersionMinor += versionMinor;
         versionString += viewer.getViewerTitle() + " version "
            + versionMajor + "." + versionMinor + "\n";
      }
      this.aboutString += "OneWireAPI version "
         + com.dalsemi.onewire.OneWireAccessProvider.getVersion() + "\n\n";
      this.aboutString += "OneWireViewer version " + owvVersionNumber
         + "." + owvVersionMajor
         + "." + owvVersionMinor + "\n"
         + versionString + "\n\n";

      String propsFile = ViewerProperties.getPropertiesFilename();
      if(propsFile.equals("onewireviewer.properties"))
      {
         propsFile = System.getProperty("user.dir") + java.io.File.separatorChar + propsFile;
      }
      this.aboutString += "Using ";
      while(propsFile.length()>50)
      {
         int index = propsFile.lastIndexOf(java.io.File.separatorChar, 50);
         this.aboutString += propsFile.substring(0, index) + " \n   ";
         propsFile = propsFile.substring(index);
      }
      this.aboutString += propsFile;

      // create the tabbed pane
      this.tabbedPane = new JTabbedPane();
      // get all the default viewers
      setDefaultViewers();

      // creates the menu bar for the frame
      createMenuBar();

      // get the polling rate from the properties file
      int pollRateIndex = ViewerProperties.getPropertyInt(POLLING_RATE_INDEX,0);
      if(pollRateIndex<0 || pollRateIndex>pollRateButtons.length)
         pollRateIndex = 0;
      // select the appropriate menu option for the current polling rate
      pollRateButtons[pollRateIndex].setSelected(true);

      // get the actual polling rate in milliseconds
      int pollRate = Integer.parseInt(
                        pollRateButtons[pollRateIndex].getActionCommand());

      //Poll device status thread
      ActionListener pollAction = new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               Component c = tabbedPane.getSelectedComponent();
               if(c instanceof Pollable)
                  ((Pollable)c).poll();
            }
         };
      pollViewer = new ThreadTimer(pollRate, pollAction, this.log);
      pollViewer.start();

      //Execute device commands thread
      ActionListener runAction = new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               Component c = tabbedPane.getSelectedComponent();
               if(c instanceof Runnable)
                  ((Runnable)c).run();
            }
         };
      runViewer = new ThreadTimer(500, runAction, this.log);
      runViewer.start();

      // List of all devices on the network
      this.deviceList = new DevicePanel(this.log);
      try
      {
         this.deviceList.setAdapterLabel("  " + adapter.getAdapterName()
                                         + " " + adapter.getPortName());
      }
      catch(Exception e)
      {
         this.deviceList.setAdapterLabel("  " + adapter.getAdapterName());
      }
      DeviceListListener dll = new DeviceListListener();
      this.deviceList.addListSelectionListener(dll);
      this.deviceList.addListDataListener(dll);

      boolean useAlternate
         = ViewerProperties.getPropertyBoolean(SHOW_ALTERNATE_NAMES, false);
      this.deviceList.setUseAlternateNames(useAlternate);
      this.alternateNamesMenuItem.setSelected(useAlternate);

      // Monitor of the network
      this.deviceMonitor = new DeviceMonitor(adapter);
      this.deviceMonitor.pauseMonitor(true);
      this.deviceMonitor.addDeviceMonitorEventListener(this.deviceList);
      Thread monitorThread = new Thread(this.deviceMonitor);
      monitorThread.start();

      // must make sure TagMonitor can control the device list and monitor
      this.tagMonitor = new TagMonitor(log);;
      this.tagMonitor.setDevicePanel(deviceList);
      this.tagMonitor.setAdapter(adapter);


      boolean bTagSearchingEnabled
         = ViewerProperties.getPropertyBoolean(ENABLE_TAG_SEARCHING, false);
      boolean bNormalSearchingEnabled
         = ViewerProperties.getPropertyBoolean(ENABLE_NORMAL_SEARCHING, true)
            || !bTagSearchingEnabled;
      if(bNormalSearchingEnabled)
      {
         this.deviceMonitor.resumeMonitor(true);
      }
      if(bTagSearchingEnabled)
      {
         this.tagMonitor.resumeMonitor(true);
         loadSavedXMLTaggingFiles();
      }

      // create the left side of the split pane, with the device list
      // and options for searching modes
      JPanel leftSidePanel = new JPanel(new BorderLayout(3,3));
      leftSidePanel.add(this.deviceList, BorderLayout.CENTER);

      JPanel checkboxGrid = new JPanel(new GridLayout(3,1));
      checkboxGrid.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "1-Wire Search Mode"));

      SearchModeListener searchModeListener = new SearchModeListener();

      normalModeEnabled = new JCheckBox("Show Normal Devices");
      normalModeEnabled.setFont(Viewer.fontPlain);
      normalModeEnabled.setSelected(bNormalSearchingEnabled);
      normalModeEnabled.addActionListener(searchModeListener);
      checkboxGrid.add(normalModeEnabled);

      taggingModeEnabled = new JCheckBox("Show Tagged Devices");
      taggingModeEnabled.setFont(Viewer.fontPlain);
      taggingModeEnabled.setSelected(bTagSearchingEnabled);
      taggingModeEnabled.addActionListener(searchModeListener);
      checkboxGrid.add(taggingModeEnabled);

      pauseSearching = new JCheckBox("Pause All Searching");
      pauseSearching.setFont(Viewer.fontPlain);
      pauseSearching.addActionListener(searchModeListener);
      checkboxGrid.add(pauseSearching);

      leftSidePanel.add(checkboxGrid, BorderLayout.SOUTH);

      // create a split pane with device list on left and viewers on right
      JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                            leftSidePanel, this.tabbedPane);
      splitPane.setOneTouchExpandable(true);
      splitPane.setDividerLocation(200);
      splitPane.addPropertyChangeListener(new SplitPaneListener());

      // take care of closing the frame
      this.addWindowListener(new WindowAdapter()
         {
            public void windowClosing(WindowEvent we)
            {
               exitApplication();
            }
         }
      );

      this.getContentPane().add(splitPane, BorderLayout.CENTER);

      //show the frame
      pack();
      setSize(ViewerProperties.getPropertyInt(
                 VIEWER_SIZE_WIDTH,VIEWER_SIZE_WIDTH_DEF),
              ViewerProperties.getPropertyInt(
                 VIEWER_SIZE_HEIGHT,VIEWER_SIZE_HEIGHT_DEF));
      setLocation(ViewerProperties.getPropertyInt(
                     VIEWER_LOCATION_X,VIEWER_LOCATION_X_DEF),
                  ViewerProperties.getPropertyInt(
                     VIEWER_LOCATION_Y,VIEWER_LOCATION_Y_DEF));

      setVisible(true);
   }

   private void loadSavedXMLTaggingFiles()
   {
      savedXMLTaggedFilesLoaded = true;
      for(int i=0; ; i++)
      {
         String filename = ViewerProperties.getProperty(XML_TAGGING_FILENAME+i);
         if(filename==null)
            break; // end for loop
         java.io.File f = new java.io.File(filename);
         if(f.exists())
         {
            try
            {
               java.io.FileInputStream fis = new java.io.FileInputStream(f);
               tagMonitor.loadTagsFromStream(adapter, fis);
            }
            catch(Exception e)
            {
               log.addError("OneWireViewer", null,
                            "Error parsing tag files: " + e);
            }
         }
      }
   }

   /**
    * Helper method that creates the menu bar for the OneWireViewer frame
    */
   private void createMenuBar()
   {
      JMenuBar menuBar = new JMenuBar();
      setJMenuBar(menuBar);

      // --------------------------------------------
      // File Menu
      JMenu menu = new JMenu("File");
      menu.setMnemonic(KeyEvent.VK_A);
      menuBar.add(menu);

         // -----------------------------------------
         // Close Menu Item
         JMenuItem menuItem = new JMenuItem("Close", KeyEvent.VK_C);
         menuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_1, ActionEvent.ALT_MASK));
         menuItem.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  exitApplication();
               }
            }
         );
         menu.add(menuItem);

      // --------------------------------------------
      // View Menu
      menu = new JMenu("View");
      menuBar.add(menu);

         // --------------------------------------------
         // Show Error Log
         menuItem = new JMenuItem("Show Message Log");
         menuItem.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  if(logFrame==null || !logFrame.isVisible())
                  {
                     if(logFrame==null)
                     {
                        logFrame = new JFrame("Message Log");
                        logFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        logFrame.getContentPane().setLayout(new BorderLayout());
                        logFrame.getContentPane().add(OneWireViewer.this.log,
                                    BorderLayout.CENTER);
                        logFrame.setSize(320, 200);

                        JMenuBar levelMenuBar = new JMenuBar();
                        logFrame.setJMenuBar(levelMenuBar);

                        JMenu levelMenu = new JMenu("Level");
                        levelMenuBar.add(levelMenu);

                        JPanel panel = new JPanel(new GridLayout(5, 1, 2, 2));
                        JCheckBoxMenuItem[] loggingLevel = new JCheckBoxMenuItem[5];
                        loggingLevel[0] = new JCheckBoxMenuItem("No Logging");
                        loggingLevel[0].setActionCommand(Integer.toString(MessageLog.NONE));
                        loggingLevel[1] = new JCheckBoxMenuItem("Errors Only");
                        loggingLevel[1].setActionCommand(Integer.toString(MessageLog.ERRMSG));
                        loggingLevel[2] = new JCheckBoxMenuItem("Warnings");
                        loggingLevel[2].setActionCommand(Integer.toString(MessageLog.WARNMSG));
                        loggingLevel[3] = new JCheckBoxMenuItem("Information");
                        loggingLevel[3].setActionCommand(Integer.toString(MessageLog.MESSAGE));
                        loggingLevel[4] = new JCheckBoxMenuItem("Verbose");
                        loggingLevel[4].setActionCommand(Integer.toString(MessageLog.VERBOSE));

                        int level = OneWireViewer.this.log.getLevel();
                        loggingLevel[level+1].setSelected(true);

                        MessageLogLevelListener mlll = new MessageLogLevelListener();
                        ButtonGroup group = new ButtonGroup();
                        for(int i=0; i<loggingLevel.length; i++)
                        {
                           group.add(loggingLevel[i]);
                           levelMenu.add(loggingLevel[i]);
                           loggingLevel[i].addActionListener(mlll);
                        }
                     }
                     logFrame.setVisible(true);
                  }
               }
            }
         );
         menu.add(menuItem);

         // --------------------------------------------
         // Menu Separator
         menu.add(new JSeparator());

         // -----------------------------------------
         // Show Tab in New Window Menu Item
         menuItem = new JMenuItem("Show Tab in New Window");
         menuItem.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  launchTabInNewWindow();
               }
            }
         );
         menu.add(menuItem);

         // --------------------------------------------
         // Menu Separator
         menu.add(new JSeparator());

         // -----------------------------------------
         // Show Tab in New Window Menu Item
         alternateNamesMenuItem
            = new JCheckBoxMenuItem("Show Device Alternate Names");
         alternateNamesMenuItem.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  OneWireViewer.this.deviceList.setUseAlternateNames(
                     alternateNamesMenuItem.isSelected());
                  ViewerProperties.setPropertyBoolean(
                     SHOW_ALTERNATE_NAMES,
                     alternateNamesMenuItem.isSelected());
                  OneWireViewer.this.deviceList.invalidate();
               }
            }
         );
         menu.add(alternateNamesMenuItem);

      // --------------------------------------------
      // Tools Menu
      menu = new JMenu("Tools");
      menuBar.add(menu);

         // --------------------------------------------
         // Set Default Adapter menu
         menuItem = new JMenuItem("Pick Adapter");
         menuItem.addActionListener(new PickAdapterListener());

         menu.add(menuItem);

         // --------------------------------------------
         // Menu Separator
         menu.add(new JSeparator());

         // --------------------------------------------
         // XML Tagging Options
         JMenu subMenu = new JMenu("XML Tagging");
         menu.add(subMenu);

            // --------------------------------------------
            // Load Tag From Desktop File
            menuItem = new JMenuItem("Load XML Tags From File");
            menuItem.addActionListener(new LoadXMLTagsListener());
            subMenu.add(menuItem);

         // --------------------------------------------
         // Menu Separator
         menu.add(new JSeparator());

         // --------------------------------------------
         // 1-Wire Speed Menu
         subMenu = new JMenu("1-Wire Speed");
         menu.add(subMenu);

            // --------------------------------------------
            // Check Box Menu Items
            deviceSpeed
               = ViewerProperties.getPropertyInt(ADAPTER_SPEED,
                  DSPortAdapter.SPEED_REGULAR);
            JCheckBoxMenuItem[] speedButtons = new JCheckBoxMenuItem[2];
            speedButtons[0] = new JCheckBoxMenuItem("Regular Speed");
            speedButtons[0].setActionCommand(Integer.toString(DSPortAdapter.SPEED_REGULAR));
            speedButtons[0].setSelected(deviceSpeed==DSPortAdapter.SPEED_REGULAR);
            speedButtons[1] = new JCheckBoxMenuItem("Overdrive Speed");
            speedButtons[1].setActionCommand(Integer.toString(DSPortAdapter.SPEED_OVERDRIVE));
            speedButtons[1].setSelected(deviceSpeed==DSPortAdapter.SPEED_OVERDRIVE);

            DeviceSpeedListener dsl = new DeviceSpeedListener();
            ButtonGroup group = new ButtonGroup();
            for(int i=0; i<speedButtons.length; i++)
            {
               speedButtons[i].addActionListener(dsl);
               group.add(speedButtons[i]);
               subMenu.add(speedButtons[i]);
            }

         // --------------------------------------------
         // Menu Separator
         menu.add(new JSeparator());

         // --------------------------------------------
         // Device Viewer Menu
         subMenu = new JMenu("Device Poll Rate");
         menu.add(subMenu);

            // --------------------------------------------
            // Poll immediately menu item
            menuItem = new JMenuItem("Poll Immediately");
            menuItem.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     pollViewer.interruptDelay();
                  }
               }
            );
            subMenu.add(menuItem);

            // --------------------------------------------
            // separator
            subMenu.addSeparator();

            // --------------------------------------------
            // Check Box Menu Items
            pollRateButtons = new JCheckBoxMenuItem[6];
            pollRateButtons[0] = new JCheckBoxMenuItem("1 Second");
            pollRateButtons[0].setActionCommand("1000");
            pollRateButtons[1] = new JCheckBoxMenuItem("5 Seconds");
            pollRateButtons[1].setActionCommand("5000");
            pollRateButtons[2] = new JCheckBoxMenuItem("10 Seconds");
            pollRateButtons[2].setActionCommand("10000");
            pollRateButtons[3] = new JCheckBoxMenuItem("30 Seconds");
            pollRateButtons[3].setActionCommand("30000");
            pollRateButtons[4] = new JCheckBoxMenuItem("1 Minute");
            pollRateButtons[4].setActionCommand("60000");
            pollRateButtons[5] = new JCheckBoxMenuItem("5 Minutes");
            pollRateButtons[5].setActionCommand("300000");

            PollRateListener pollRateListener = new PollRateListener();
            pollRateButtonGroup = new ButtonGroup();
            for(int i=0; i<pollRateButtons.length; i++)
            {
               pollRateButtonGroup.add(pollRateButtons[i]);
               subMenu.add(pollRateButtons[i]);
               pollRateButtons[i].addActionListener(pollRateListener);
            }

      // --------------------------------------------
      // Help Menu
      menu = new JMenu("Help");
      menuBar.add(menu);

         // --------------------------------------------
         // About Menu Item
         menuItem = new JMenuItem("About");
         menuItem.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  showMessageDialog(OneWireViewer.this, "About", aboutString);
               }
            }
         );
         menu.add(menuItem);
   }

   /**
    * Clears the current viewer list and adds the default viewers.
    */
   public void setDefaultViewers()
   {
      tabbedPane.removeAll();

      // get all the default viewers
      Enumeration enum = viewerFactory.getDefaultViewers();
      while(enum.hasMoreElements())
      {
         Viewer viewer = (Viewer)enum.nextElement();
         tabbedPane.addTab(viewer.getViewerTitle(), null, viewer,
                           viewer.getViewerDescription());
      }
      this.tabbedPane.revalidate();
   }

   /**
    * Method clears any viewers from the tab.  Used when "unselecting" a device in
    * the device list.
    */
   public void setNoDevice()
   {
      // reset the title
      setTitle(strTitle);

      // get the total number of viewers and close each one
      int totalTabs = tabbedPane.getTabCount();
      for(int i=totalTabs-1; i>=0; i--)
      {
         Viewer viewer = (Viewer)tabbedPane.getComponentAt(i);
         String text = "   Allowing " + viewer.getViewerTitle() +
                       " Tab to finish pending tasks   ";

         // wait for each viewer to finish it's work
         int timeout = BUSY_TIMEOUT;
         while(viewer.isBusy() && timeout>0)
         {
            showBusyDialog(text);
            tabbedPane.setSelectedIndex(i);
            try
            {
               Thread.sleep(100);
            }
            catch(InterruptedException ie)
            {;}
            timeout--;
         }
         if(viewer.isBusy())
         {
            try
            {
               pollViewer.join();
            }
            catch(InterruptedException ie)
            {}
         }
         hideBusyDialog();

         if(!viewerFactory.isDefaultViewer(viewer))
            tabbedPane.remove(i);

         viewer.clearContainer();
      }
      this.tabbedPane.setSelectedIndex(0);
      this.tabbedPane.revalidate();
   }

   /**
    * @param viewer
    * @param tabbedPaneIndex
    * @param displayText
    */
   private void waitForViewer(Viewer viewer, int tabbedPaneIndex, String displayText)
   {

   }

   /**
    * Clones the currently display tab and launches it in a new window, with
    * it's own thread timers for both polling and running the Viewer
    *
    * @see Viewer#cloneViewer()
    * @see ThreadTimer
    */
   public void launchTabInNewWindow()
   {
      Viewer c = (Viewer)tabbedPane.getSelectedComponent();

      // make sure the viewer is cloneable, otherwise, don't display in new window
      if(!c.isCloneable())
         return;

      DefaultListModel lm = deviceList.getListModel();
      Object o = lm.get(deviceList.getSelectedIndex());
      String popupTitle = c.getViewerTitle()
         + " - " + o;
      if(frameHash.containsKey(popupTitle))
      {
         ((JFrame)frameHash.get(popupTitle)).requestFocus();
      }
      else
      {
         Viewer viewer = c.cloneViewer();

         if(viewer instanceof Pollable)
         {
            ThreadTimer timer = new ThreadTimer(this.pollViewer.getDelay(),
               new PollActionListener((Pollable)viewer), this.log);
            timer.start();
            pollThreadHash.put(popupTitle, timer);
         }
         if(viewer instanceof Runnable)
         {
            ThreadTimer timer = new ThreadTimer(500,
               new RunActionListener((Runnable)viewer), this.log);
            timer.start();
            runThreadHash.put(popupTitle, timer);
         }
         JFrame frame = new JFrame(popupTitle);
         frame.getContentPane().add(viewer);
         frame.setSize(c.getSize());
         frame.addWindowListener(new PopupWindowAdapter(popupTitle));
         frame.setVisible(true);
         frameHash.put(popupTitle, frame);
      }

      tabbedPane.setSelectedIndex(0);
   }


   public void hideBusyDialog()
   {
      if(dlgBusy!=null)
      {
         dlgBusy.setVisible(false);
      }
   }

   public void showBusyDialog(String message)
   {
      if(dlgBusy==null)
      {
         lblBusyText = new JLabel(message,JLabel.CENTER);
         lblBusyText.setFont(Viewer.fontBold);
         dlgBusy = new JDialog(this, "Please wait...", false);
         dlgBusy.getContentPane().setLayout(new BorderLayout(15,15));
         dlgBusy.getContentPane().add(lblBusyText, BorderLayout.CENTER);
         dlgBusy.setResizable(false);
      }
      if(!dlgBusy.isVisible())
      {
         // update the busy label
         lblBusyText.setText(message);
         // determine optimum size
         dlgBusy.pack();
         // center the dialog
         Dimension frameSize = this.getSize();
         Dimension dlgSize = dlgBusy.getSize();
         Point location = this.getLocation();
         location.x += (frameSize.width/2)-(dlgSize.width/2);
         location.y += (frameSize.height/2)-(dlgSize.height/2);
         dlgBusy.setLocation(location);
         // display the dialog
         dlgBusy.setVisible(true);
      }
      // make sure it gets repainted (busy dialog is usually shown during
      // an Event handler, so must force repaint).
      dlgBusy.update(dlgBusy.getGraphics());
   }

   /**
    * Exits the application, ensuring that all application preferences are
    * stored in the Viewer Properties file and all threads have ended.
    *
    * @see ViewerProperties
    * @see ThreadTimer#killTimer()
    */
   public void exitApplication()
   {
      System.out.println("Exiting...");

      // save the viewer's polling rate
      int index = 0;
      for(; index<pollRateButtons.length; index++)
      {
         if(pollRateButtons[index].isSelected())
            break;
      }
      ViewerProperties.setPropertyInt(POLLING_RATE_INDEX,
            (index==pollRateButtons.length) ? 0 : index);

      // Save other persistant viewer properties
      ViewerProperties.setPropertyInt(MESSAGE_LOG_LEVEL, this.log.getLevel());
      ViewerProperties.setPropertyInt(VIEWER_SIZE_WIDTH, getSize().width);
      ViewerProperties.setPropertyInt(VIEWER_SIZE_HEIGHT, getSize().height);
      ViewerProperties.setPropertyInt(VIEWER_LOCATION_X, getLocation().x);
      ViewerProperties.setPropertyInt(VIEWER_LOCATION_Y, getLocation().y);
      ViewerProperties.setProperty(LOOK_AND_FEEL,
                              UIManager.getLookAndFeel().getClass().getName());
      ViewerProperties.setPropertyBoolean(ENABLE_NORMAL_SEARCHING,
                                          this.normalModeEnabled.isSelected());
      ViewerProperties.setPropertyBoolean(ENABLE_TAG_SEARCHING,
                                          this.taggingModeEnabled.isSelected());
      ViewerProperties.setPropertyInt(ADAPTER_SPEED, deviceSpeed);
      // persist this property
      ViewerProperties.setPropertyBoolean(ALLOW_PURE_SERIAL_ADAPTER,
               ViewerProperties.getPropertyBoolean(ALLOW_PURE_SERIAL_ADAPTER, false));

      ViewerProperties.saveProperties();

      // wait for all viewers to finish their work.
      setNoDevice();
      deviceMonitor.killMonitor();

      // clean up all the threads
      pollViewer.killTimer(false);
      runViewer.killTimer(false);

      // clean up the independent polling threads, for windowed viewers
      java.util.Iterator i = pollThreadHash.values().iterator();
      while(i.hasNext())
         ((ThreadTimer)i.next()).killTimer(false);

      // clean up the independent running threads, for windowed viewers
      i = runThreadHash.values().iterator();
      while(i.hasNext())
         ((ThreadTimer)i.next()).killTimer(false);

      try
      {
         deviceMonitor.getAdapter().freePort();
      }
      catch(OneWireException ioe)
      {
         System.err.println("Failed to free port for adapter");
         ioe.printStackTrace();
      }

      // quit the application
      System.exit(0);
   }

   private class PickAdapterListener implements ActionListener
   {
      public void actionPerformed(ActionEvent ae)
      {
         boolean isRunning = deviceMonitor.isMonitorRunning();
         if(isRunning)
         {
            showBusyDialog("Pausing Device Monitor");
            while(isRunning && !deviceMonitor.pauseMonitor(false))
            {
               showBusyDialog("Pausing Device Monitor. . .");
               try
               {
                  Thread.sleep(100);
               }
               catch(InterruptedException ie)
               {;}
            }
            hideBusyDialog();
         }
         isRunning = tagMonitor.isMonitorRunning();
         if(isRunning)
         {
            showBusyDialog("Pausing Tagging Monitor");
            while(isRunning && !tagMonitor.pauseMonitor(false))
            {
               showBusyDialog("Pausing Tagging Monitor. . .");
               try
               {
                  Thread.sleep(100);
               }
               catch(InterruptedException ie)
               {;}
            }
            hideBusyDialog();
         }
         showBusyDialog("Loading Adapter List. . .");
         AdapterChooser chooser = new AdapterChooser(
            ViewerProperties.getProperty(ADAPTER_NAME),
            ViewerProperties.getProperty(ADAPTER_PORT),
            Viewer.fontBold, Viewer.fontPlain);
         hideBusyDialog();

         int retVal = JOptionPane.showConfirmDialog(OneWireViewer.this,
            chooser, "Please pick an adapter",
            JOptionPane.OK_CANCEL_OPTION);
         if(retVal == JOptionPane.OK_OPTION)
         {
            DSPortAdapter newAdapter = null;
            String adapterName = chooser.getAdapterName();
            String adapterPort = chooser.getAdapterPort();
            if(adapterName.length()>0 && adapterPort.length()>0)
            {
               try
               {
                  newAdapter =
                     OneWireAccessProvider.getAdapter(adapterName, adapterPort);
                  if(newAdapter.adapterDetected())
                  {
                     setNoDevice();
                     try
                     {
                        OneWireViewer.this.deviceMonitor.getAdapter().freePort();
                     }
                     catch(OneWireException ioe)
                     {
                        System.err.println("Failed to free port for adapter");
                        ioe.printStackTrace();
                     }
                     OneWireViewer.this.deviceMonitor.setAdapter(newAdapter);
                     try
                     {
                        OneWireViewer.this.deviceList.setAdapterLabel("  " + newAdapter.getAdapterName()
                                                        + " " + newAdapter.getPortName());
                     }
                     catch(Exception e)
                     {
                        OneWireViewer.this.deviceList.setAdapterLabel("  " + newAdapter.getAdapterName());
                     }

                     tagMonitor.setAdapter(newAdapter);

                     ViewerProperties.setProperty(OneWireViewer.ADAPTER_NAME,
                                                    adapterName);
                     ViewerProperties.setProperty(OneWireViewer.ADAPTER_PORT,
                                                    adapterPort);
                     JOptionPane.showMessageDialog(OneWireViewer.this,
                        new JLabel("Loaded Adapter Successfully!"));
                  }
               }
               catch(OneWireException owe)
               {
                  newAdapter = null;
                  JTextArea ta = new JTextArea("Failed To Load Adapter!\n"
                        + owe.getMessage());
                  ta.setEditable(false);
                  ta.setWrapStyleWord(true);
                  ta.setBackground(Color.lightGray);
                  ta.setFont(Viewer.fontBold);
                  JOptionPane.showMessageDialog(OneWireViewer.this, ta,
                            "Failed", JOptionPane.ERROR_MESSAGE);
               }
            }
         }
         if(normalModeEnabled.isSelected())
            deviceMonitor.resumeMonitor(true);
         if(taggingModeEnabled.isSelected())
            tagMonitor.resumeMonitor(true);
      }
   }

   private class SearchModeListener implements ActionListener
   {
      Vector savedTaggedDevices = new Vector();
      Vector savedNormalDevices = new Vector();
      public void actionPerformed(ActionEvent ae)
      {
         Object source = ae.getSource();
         if(source==pauseSearching)
         {
            if(pauseSearching.isSelected())
            {
               tagMonitor.pauseMonitor(true);
               deviceMonitor.pauseMonitor(true);
            }
            else
            {
               if(normalModeEnabled.isSelected())
                  deviceMonitor.resumeMonitor(true);
               if(taggingModeEnabled.isSelected())
                  tagMonitor.resumeMonitor(true);
            }
         }
         else if(source==normalModeEnabled || source==taggingModeEnabled)
         {
            // need to update the device list at some point, with hidden objects
            DefaultListModel dlm = deviceList.getListModel();
            boolean bDoTag = false, bDoNormal = false;

            // if neither is selected, select the one
            // that wasn't just unselected
            if(!normalModeEnabled.isSelected() && !taggingModeEnabled.isSelected())
            {
               if(source==normalModeEnabled)
               {
                  taggingModeEnabled.setSelected(true);
                  bDoTag = true;
               }
               else
               {
                  normalModeEnabled.setSelected(true);
                  bDoNormal = true;
               }
            }

            if(normalModeEnabled.isSelected()
               && (source==normalModeEnabled || bDoNormal))
            {
               deviceMonitor.resumeMonitor(true);
               for(int i=0; i<savedNormalDevices.size(); i++)
                  dlm.addElement(savedNormalDevices.elementAt(i));
               savedNormalDevices.clear();
            }
            else if(source==normalModeEnabled)
            {
               deviceMonitor.pauseMonitor(true);
               for(int i=0; i<dlm.getSize();)
               {
                  Object o = dlm.getElementAt(i);
                  if(o instanceof OneWireContainer)
                  {
                     dlm.removeElementAt(i);
                     savedNormalDevices.addElement(o);
                  }
                  else
                  {
                     i = i + 1;
                  }
               }
            }
            if(taggingModeEnabled.isSelected()
               && (source==taggingModeEnabled || bDoTag))
            {
               tagMonitor.resumeMonitor(true);
               if(!savedXMLTaggedFilesLoaded)
                  loadSavedXMLTaggingFiles();
               for(int i=0; i<savedTaggedDevices.size(); i++)
                  dlm.addElement(savedTaggedDevices.elementAt(i));
               savedTaggedDevices.clear();
            }
            else if(source==taggingModeEnabled)
            {
               tagMonitor.pauseMonitor(true);
               for(int i=0; i<dlm.getSize();)
               {
                  Object o = dlm.getElementAt(i);
                  if(o instanceof TaggedDevice)
                  {
                     dlm.removeElementAt(i);
                     savedTaggedDevices.addElement(o);
                  }
                  else
                  {
                     i = i + 1;
                  }
               }
            }
         }
      }
   }

   private class PollRateListener implements ActionListener
   {
      public void actionPerformed(ActionEvent ae)
      {
         pollViewer.setDelay(Integer.parseInt(
                              ae.getActionCommand()));
         pollViewer.interruptDelay();

         Iterator i = pollThreadHash.values().iterator();
         while(i.hasNext())
         {
            ThreadTimer timer = (ThreadTimer)i.next();
            timer.setDelay(pollViewer.getDelay());
            timer.interruptDelay();
         }
      }
   };

   private class RunActionListener implements ActionListener
   {
      private Runnable r = null;
      public RunActionListener(Runnable runnable)
      {
         this.r = runnable;
      }
      public void actionPerformed(ActionEvent ae)
      {
         r.run();
      }
   }

   private class PollActionListener implements ActionListener
   {
      private Pollable p = null;
      public PollActionListener(Pollable pollable)
      {
         this.p = pollable;
      }
      public void actionPerformed(ActionEvent ae)
      {
         p.poll();
      }
   }

   private class PopupWindowAdapter extends WindowAdapter
   {
      private String title = null;
      public PopupWindowAdapter(String title)
      {
         this.title = title;
      }
      public void windowClosing(WindowEvent we)
      {
         frameHash.remove(title);
         runThreadHash.remove(title);
         pollThreadHash.remove(title);
      }
   }

   private class DeviceSpeedListener implements ActionListener
   {
      public void actionPerformed(ActionEvent ae)
      {
         int deviceSpeed = Integer.parseInt(ae.getActionCommand());
         OneWireViewer.this.deviceSpeed = deviceSpeed;
         OneWireContainer owc = OneWireViewer.this.deviceList.getSelectedContainer();
         if(owc!=null && owc.getMaxSpeed()>=deviceSpeed)
            owc.setSpeed(deviceSpeed, true);
      }
   }

   private class SplitPaneListener implements java.beans.PropertyChangeListener
   {
      public void propertyChange(java.beans.PropertyChangeEvent evt)
      {
         JSplitPane sp = (JSplitPane)evt.getSource();
         if(sp.getDividerLocation()==1)
         {
            deviceMonitor.pauseMonitor(true);
         }
         else if(sp.getLastDividerLocation()==1 && !pauseSearching.isSelected())
         {
            deviceMonitor.resumeMonitor(true);
         }
      }
   }

   private class LoadXMLTagsListener implements ActionListener
   {
      public void actionPerformed(ActionEvent ae)
      {
         FileDialog fd = new FileDialog(OneWireViewer.this,
            "Load XML Tags From File", FileDialog.LOAD);
         fd.setVisible(true);
         String filename = fd.getFile();
         if(filename!=null)
         {
            String directory = fd.getDirectory();
            java.io.File f = new java.io.File(directory, filename);
            if(f.exists())
            {
               try
               {
                  java.io.FileInputStream fis = new java.io.FileInputStream(f);
                  tagMonitor.loadTagsFromStream(adapter, fis);

                  int ret = JOptionPane.showConfirmDialog(OneWireViewer.this,
                     new JLabel("Add tagging file to saved list of files?"),
                     "Save Tagging File", JOptionPane.YES_NO_OPTION);
                  if(ret==JOptionPane.YES_OPTION)
                  {
                     JOptionPane.showMessageDialog(OneWireViewer.this,
                        new JLabel("To remove this entry later, " +
                                   "edit this file: " +
                                   ViewerProperties.getPropertiesFilename()));
                     String fullFilename = directory + filename;
                     String filenameList = null;
                     int i = -1;
                     do
                     {
                        i += 1;
                        filenameList =
                           ViewerProperties.getProperty(XML_TAGGING_FILENAME+i);
                     }
                     while(filenameList!=null && !filenameList.equals(fullFilename))
                        /*no-op*/;
                     if(filenameList==null)
                        ViewerProperties.setProperty(XML_TAGGING_FILENAME+i,
                                                     fullFilename);
                  }
               }
               catch(Exception e)
               {
                  log.addError("OneWireViewer", null,
                               "Error loading file: " + e.toString());
               }
            }
         }
      }
   }

   /* -------------------------------------------------------------------- */
   /* Message Log Level Listener Class */
   /* -------------------------------------------------------------------- */

   /**
    * Inner class that handles changes to the mesage log level
    */
   private class MessageLogLevelListener implements ActionListener
   {
      public void actionPerformed(ActionEvent ae)
      {
         int level = Integer.parseInt(ae.getActionCommand());
         log.setLevel(level);
      }
   }
   /* -------------------------------------------------------------------- */

   /* -------------------------------------------------------------------- */
   /* List Selection Listener Class */
   /* -------------------------------------------------------------------- */

   /**
    * Inner class for monitoring activity on the DeviceList.  When a new device
    * is selected, this monitor will adjust the Viewers available accordingly.
    */
   private class DeviceListListener implements ListSelectionListener,ListDataListener
   {
      public void valueChanged(ListSelectionEvent lse)
      {
         if (lse.getValueIsAdjusting())
            return;

         // get only the default viewers
         setNoDevice();
         if (!deviceList.isSelectionEmpty())
         {
            /* selected a new device, update the tabs */
            DefaultListModel lm = deviceList.getListModel();
            Object o = lm.get(deviceList.getSelectedIndex());
            TaggedDevice td = null;
            OneWireContainer owc = null;
            Enumeration e = null;
            if(o instanceof TaggedDevice)
            {
               td = (TaggedDevice)o;
               owc = td.getDeviceContainer();
               e = viewerFactory.getViewers(td);
            }
            else
            {
               owc = (OneWireContainer)o;
               e = viewerFactory.getViewers(owc);
            }

            if(OneWireViewer.this.deviceSpeed<=owc.getMaxSpeed())
               owc.setSpeed(OneWireViewer.this.deviceSpeed, true);
            else
               owc.setSpeed(owc.getMaxSpeed(), true);
            setTitle(strTitle + " - " + owc.toString());

            while(e.hasMoreElements())
            {
               Viewer viewer = (Viewer)e.nextElement();
               // don't re-add default viewers
               if(!viewerFactory.isDefaultViewer(viewer))
               {
                  tabbedPane.addTab(viewer.getViewerTitle(), null, viewer,
                                    viewer.getViewerDescription());
               }
            }
            tabbedPane.setSelectedIndex(0);
            tabbedPane.revalidate();
            //tabbedPane.updateUI();

            /* Interrupt threads for an immediate attention to the current device */
            runViewer.interruptDelay();
            pollViewer.interruptDelay();
         }
      }

      public void intervalRemoved(ListDataEvent e)
      {
         int index = deviceList.getSelectedIndex();
         if ( index>=e.getIndex0() && index<=e.getIndex1())
         {
            // get only the default viewers
            setDefaultViewers();
         }
      }

      public void intervalAdded(ListDataEvent e)
      { /*no-op*/; }
      public void contentsChanged(ListDataEvent e)
      { /*no-op*/; }
   }
   /* -------------------------------------------------------------------- */

   /**
    * Display's a modal dialog window with the given title and message.
    * message is displayed in a text area placed in a scroll pane.
    *
    * @param parent the parent Component for the Dialog
    * @param title the title for the Dialog
    * @param message the message to display in the text area for the Dialog
    */
   public static void showMessageDialog(Component parent, String title, String message)
   {
      // create the text area
      JTextArea ta = new JTextArea();
      ta.setFont(Viewer.fontPlain);
      ta.setText(message);

      // create the scroll pane
      JScrollPane p = new JScrollPane(ta);
      p.setBorder(BorderFactory.createEtchedBorder());

      // create the dialog
      JOptionPane pane = new JOptionPane(p, JOptionPane.INFORMATION_MESSAGE,
         JOptionPane.DEFAULT_OPTION);
      JDialog dlg = pane.createDialog(parent, title);

      // display the dialog
      dlg.setVisible(true);
   }

   /**
    * Main - loads properties file, loads default adapter, then creates new
    * viewer frame to display.
    */
   public static void main(String[] args)
   {
      // temporary JFrame for dialog windows, if necessary
      JFrame tempFrame = new JFrame();

      try
      {
         /* Workaround for bug in Java Web Start 1.0.1 - Access to Serial
            Communications API causes NullPointerException in reflection
            call.  Removing the Security Manager removes the symptom. */
         System.setSecurityManager(null);

         // load the persistent properties
         if(!ViewerProperties.loadProperties() ||
            ViewerProperties.getPropertyInt(OWV_VERSION, 0)<owvVersionNumber)
         {
            // no properties file found, let's run the setup app
            OneWireSetup setup = new OneWireSetup(tempFrame, true);
            setup.setVisible(true);
            if(!setup.isCanceled())
               ViewerProperties.setPropertyInt(OWV_VERSION, owvVersionNumber);
         }

         // create the port adapter
         DSPortAdapter adapter = null;
         try
         {
            String adapterName = ViewerProperties.getProperty(ADAPTER_NAME);
            String adapterPort = ViewerProperties.getProperty(ADAPTER_PORT);
            if(adapterName==null || adapterPort==null)
               adapter = OneWireAccessProvider.getDefaultAdapter();
            else
               adapter = OneWireAccessProvider.getAdapter(adapterName, adapterPort);

            System.out.println("Found adapter: " + adapter.getAdapterName());
            System.out.println("      on port: " + adapter.getPortName());
         }
         catch(Throwable t)
         {
            try
            {
               AdapterChooser chooser = new AdapterChooser(
                     ViewerProperties.getProperty(ADAPTER_NAME),
                     ViewerProperties.getProperty(ADAPTER_PORT),
                     Viewer.fontBold, Viewer.fontPlain);
               JOptionPane.showMessageDialog(tempFrame, chooser);

               String adapterName = chooser.getAdapterName();
               String adapterPort = chooser.getAdapterPort();
               if( (adapterName==null || adapterName.length()==0) ||
                   (adapterPort==null || adapterPort.length()==0) )
                  adapter = OneWireAccessProvider.getDefaultAdapter();
               else
                  adapter = OneWireAccessProvider.getAdapter(adapterName, adapterPort);

               System.out.println("Found adapter: " + adapter.getAdapterName());
               System.out.println("      on port: " + adapter.getPortName());

               ViewerProperties.setProperty(OneWireViewer.ADAPTER_NAME,
                                              adapterName);
               ViewerProperties.setProperty(OneWireViewer.ADAPTER_PORT,
                                              adapterPort);
            }
            catch(Throwable t2)
            {
               String errString = t2.getMessage() + "\n"
                 + "* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *\n"
                 + "Cannot load default adapter. Please check that the adapter\n"
                 + "is connected and that no other application is using the\n"
                 + "port.\n"
                 + "\n"
                 + "If you have checked the adapter, the default port might\n"
                 + "not be selected.  Either set the value of the TMEX default\n"
                 + "using the utility provided with the TMEX Runtime, or create\n"
                 + "a new file in the <java home>/lib folder called \n"
                 + "onewireviewer.properties with two lines, similar to the\n"
                 + "following:\n"
                 + "\n"
                 + "#native win32 drivers"
                 + " " + ADAPTER_NAME + "={DS9097U}\n "
                 + " " + ADAPTER_PORT + "=COM1\n"
                 + "#or javax.comm drivers"
                 + " " + ADAPTER_NAME + "=DS9097U\n "
                 + " " + ADAPTER_PORT + "=COM1\n"
                 + "\n"
                 + "The full path to this file should be:\n"
                 + " " + System.getProperty("java.home")
                    + java.io.File.separator + "lib" + java.io.File.separator
                    + "onewireviewer.properties";
               showMessageDialog(tempFrame, "Exception Occurred", errString);
               System.exit(1);
            }
         }


         // try to set the specified look and feel
         try
         {
            UIManager.setLookAndFeel(ViewerProperties.getProperty(LOOK_AND_FEEL,
                                                            LOOK_AND_FEEL_DEF));
         }
         catch(Exception e)
         {
            showMessageDialog(tempFrame, "Exception Occurred", e.toString());
         }

         // create the OneWireViewer
         OneWireViewer oneWireViewer = new OneWireViewer(adapter);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         showMessageDialog(tempFrame, "Exception Occurred", e.toString());
      }
      // free up resources from temporary JFrame
      tempFrame.dispose();
   }
}
