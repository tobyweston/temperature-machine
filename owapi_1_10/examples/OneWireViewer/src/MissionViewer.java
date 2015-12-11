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

import java.util.*;
import javax.swing.*;
import javax.swing.SwingConstants;
import java.awt.event.*;
import java.awt.*;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.container.MissionContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.utils.Convert;
import com.dalsemi.onewire.container.OneWireContainer41;


/**
 * A Thermochron (DS1921) Viewer for integration in the OneWireViewer.
 * All Thermochron (DS1921) devices are supported by this viewer.  This
 * viewer displays the current mission information as well as the temperature
 * log, the histogram, and the alarm log.
 *
 * @author SH
 * @version 1.00
 */
public class MissionViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "MissionViewer";
   private static final String strTab = "Mission";
   private static final String strTip = "Inits a missionable device or displays mission status/results";
   private static final String naString = "N/A";

   public static final String MISSION_VIEWER_FAHRENHEIT = "mission.viewer.displayFahrenheit";

   /* container variables */
   private MissionContainer container = null;
   private TaggedDevice taggedDevice = null;

   private JButton[] cmdButton;
   private static final int TOTAL_BUTTONS = 6;
   private static final int REFRESH_BTN = 0;
   private static final int START_BTN = 1;
   private static final int STOP_BTN = 2;
   private static final int SFT_PSW_BTN = 3;
   private static final int DEV_PSW_BTN = 4;
   private static final int EN_PSW_BTN = 5;

   /* visual components */
   public Plot temperaturePlot = null, dataPlot = null;
   private JTabbedPane tabbedResultsPane = null;
   private JScrollPane featureScroll = null,
                       temperatureScroll = null,
                       dataScroll = null;
   //private JTextArea alarmHistory = null;
   //private JTextArea histogram = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = {
      "Mission in Progress? ", "SUTA Mission? ", "Waiting for Temperature Alarm? ",
      "Sample Rate: ", "Mission Start Time: ", "Mission Sample Count: ",
      "Roll Over Enabled? ", "First Sample Timestamp: ",
      "Total Mission Samples: ", "Total Device Samples: ",
      "Temperature Logging: ", "Temperature High Alarm: ", "Temperature Low Alarm: ",
      "Data Logging: ", "Data High Alarm: ", "Data Low Alarm: "

   };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 16;
   private static final int IS_ACTIVE = 0;
   private static final int MISSION_SUTA = 1;
   private static final int MISSION_WFTA = 2;
   private static final int SAMPLE_RATE = 3;
   private static final int MISSION_START = 4;
   private static final int MISSION_SAMPLES = 5;
   private static final int ROLL_OVER = 6;
   private static final int FIRST_SAMPLE_TIMESTAMP = 7;
   private static final int TOTAL_SAMPLES = 8;
   private static final int DEVICE_SAMPLES = 9;
   private static final int TEMP_LOGGING = 10;
   private static final int TEMP_HIGH_ALARM = 11;
   private static final int TEMP_LOW_ALARM = 12;
   private static final int DATA_LOGGING = 13;
   private static final int DATA_HIGH_ALARM = 14;
   private static final int DATA_LOW_ALARM = 15;

   private volatile boolean pausePoll = false, pollRunning = false;
   private boolean bFahrenheit = false;

   private static final java.text.NumberFormat nf =
      new java.text.DecimalFormat();
   private static final java.text.DateFormat df =
      java.text.DateFormat.getDateTimeInstance(
         java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM);


   /* single-instance Viewer Tasks */
   private final SetupViewerTask setupViewerTask = new SetupViewerTask();

   private String[] channelLabel = null;
   private boolean[] hasResolution = null;
   private double[][] channelResolution = null;

   public MissionViewer()
   {
      super(strTitle);

      bFahrenheit = ViewerProperties.getPropertyBoolean(
         MISSION_VIEWER_FAHRENHEIT, false);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 7;

      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);

      // tabbed results pane
      // This pane consists of the status panel, the temperature panel,
      // the histogram panel, and the alarm history panel.
      tabbedResultsPane = new JTabbedPane(SwingConstants.TOP);
         // feature panel
         JPanel featurePanel = new JPanel(new GridLayout(TOTAL_FEATURES, 2, 3, 3));
         featureScroll = new JScrollPane(featurePanel,
                          JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                          JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
               lblFeature[i].setFont(fontPlain);
               lblFeature[i].setForeground(Color.black);
               lblFeature[i].setBackground(Color.lightGray);

               featurePanel.add(lblFeatureHdr[i]);
               featurePanel.add(lblFeature[i]);
            }

         // Temperature panel
         JPanel dataPanel = new JPanel(new BorderLayout());
         dataScroll = new JScrollPane(dataPanel,
                                      JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                      JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            dataPlot = new Plot();
            dataPlot.setScale(30, 65);
         dataPanel.add(dataPlot, BorderLayout.CENTER);
         // Temperature panel
         JPanel temperaturePanel = new JPanel(new BorderLayout());
         temperatureScroll = new JScrollPane(temperaturePanel,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            temperaturePlot = new Plot();
            temperaturePlot.setScale(60, 85);
         temperaturePanel.add(temperaturePlot, BorderLayout.CENTER);
//         // Histogram panel
//         JPanel histogramPanel = new JPanel(new BorderLayout());
//         JScrollPane histogramScroll = new JScrollPane(histogramPanel,
//                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//            histogram = new JTextArea();
//            histogram.setEditable(false);
//            histogram.setFont(fontPlain);
//         histogramPanel.add(histogram, BorderLayout.CENTER);
//         // Alarm History panel
//         JPanel alarmHistoryPanel = new JPanel(new BorderLayout());
//         JScrollPane alarmHistoryScroll = new JScrollPane(alarmHistoryPanel,
//                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
//                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//            alarmHistory = new JTextArea();
//            alarmHistory.setEditable(false);
//            alarmHistory.setFont(fontPlain);
//         alarmHistoryPanel.add(alarmHistory, BorderLayout.CENTER);

      // add above panels to the tabbedResultsPane
      //tabbedResultsPane.addTab("Status",null,featureScroll, "Mission Status");
      //tabbedResultsPane.addTab("Temperature",null,temperatureScroll,"Graphs the mission's temperature log");
      //tabbedResultsPane.addTab("Data",null,dataScroll,"Graphs the mission's data log");
      //tabbedResultsPane.addTab("Histogram",null,histogramScroll,"Graphs the mission's temperature histogram");
      //tabbedResultsPane.addTab("Alarm Log",null,alarmHistoryScroll, "Shows a mission's alarm history");

      cmdButton = new JButton[TOTAL_BUTTONS];
      // Refresh Panel for Refresh Button.
      JPanel commandPanel = new JPanel(new GridLayout(3,1,3,3));
      JScrollPane commandScroll = new JScrollPane(commandPanel);
      commandPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Command"));
         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         buttonPanel.setBackground(Color.lightGray);
            cmdButton[REFRESH_BTN] = new JButton("Refresh Mission Results");
            cmdButton[REFRESH_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     for(int btn=0; btn<TOTAL_BUTTONS; btn++)
                        cmdButton[btn].setEnabled(false);
                     setupViewerTask.reloadResults = true;
                     enqueueRunTask(setupViewerTask);
                  }
               }
            );
            cmdButton[START_BTN] = new JButton("Start New Mission");
            cmdButton[START_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     InitMissionPanel imp = new InitMissionPanel(channelLabel,
                        hasResolution, channelResolution);
                     int result = JOptionPane.showConfirmDialog(MissionViewer.this, imp,
                        "Initialize New Mission", JOptionPane.OK_CANCEL_OPTION);
                     if(result!=JOptionPane.CANCEL_OPTION)
                     {
                        boolean[] channelEnabled, enableAlarm;
                        boolean rollover, syncClock, suta;
                        int rate, delay;
                        double[] low, high, resolution;

                        boolean atLeastOneEnabled = false;
                        channelEnabled = new boolean[imp.chkChannelEnabled.length];
                        for(int i=0; i<channelEnabled.length; i++)
                        {
                           channelEnabled[i] = imp.chkChannelEnabled[i].isSelected();
                           atLeastOneEnabled |= channelEnabled[i];
                        }
                        if(!atLeastOneEnabled)
                        {
                           setStatus(ERRMSG, "No channels enabled for mission.");
                           return;
                        }

                        rollover = imp.chkRollover.isSelected();
                        syncClock = imp.chkSyncClock.isSelected();
                        suta = imp.chkSUTA.isSelected();

                        try
                        {
                           rate = Integer.parseInt(imp.txtSampleRate.getText());
                           delay = Integer.parseInt(imp.txtStartDelay.getText());
                           low = new double[imp.txtLowAlarm.length];
                           high = new double[low.length];
                           enableAlarm = new boolean[low.length];
                           resolution = new double[low.length];
                           for(int i=0; i<low.length; i++)
                           {
                              enableAlarm[i] = imp.chkAlarmEnabled[i].isSelected();
                              if(enableAlarm[i])
                              {
                                 low[i] = Double.parseDouble(imp.txtLowAlarm[i].getText());
                                 high[i] = Double.parseDouble(imp.txtHighAlarm[i].getText());
                              }
                              resolution[i] = Double.parseDouble(imp.lstResolution[i].getSelectedValue().toString());
                              if(bFahrenheit)
                              {
                                 low[i] = Convert.toCelsius(low[i]);
                                 high[i] = Convert.toCelsius(high[i]);
                              }
                           }
                        }
                        catch(NumberFormatException nfe)
                        {
                           setStatus(ERRMSG, "Bad Number Format: " + nfe);
                           return;
                        }

                        synchronized(syncObj)
                        {
                           for(int btn=0; btn<TOTAL_BUTTONS; btn++)
                              cmdButton[btn].setEnabled(false);
                           InitMissionTask imt = new InitMissionTask(adapter, container,
                                    channelEnabled,
                                    rollover, syncClock, suta, rate,
                                    delay, enableAlarm, low, high, resolution);
                           if(imp.oneSecMissionTest.isSelected())
                           {
                              enqueueRunTask(new DelayedInitMissionTask(imt));
                           }
                           else
                           {
                              enqueueRunTask(imt);
                              enqueueRunTask(setupViewerTask);
                           }
                        }
                     }
                  }
               }
            );
            cmdButton[STOP_BTN] = new JButton("Disable Mission");
            cmdButton[STOP_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        for(int i=0; i<TOTAL_BUTTONS; i++)
                           cmdButton[i].setEnabled(false);
                        enqueueRunTask(new DisableMissionTask(adapter, container));
                        enqueueRunTask(setupViewerTask);
                     }
                  }
               }
            );
            /*JButton rangeButton = new JButton("Fewer Results");
            rangeButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     int iOffset = 0;
                     int iLength = dataPlot.getPoints().length;

                     JPanel rangePanel = new JPanel();
                     rangePanel.setLayout(new BoxLayout(rangePanel, BoxLayout.X_AXIS));
                     rangePanel.add(new JLabel(" offset = "));
                     JTextField offset = new JTextField("" + iOffset);
                     rangePanel.add(offset);
                     rangePanel.add(new JLabel(" length = "));
                     JTextField length = new JTextField("" + iLength);
                     rangePanel.add(length);

                     int result = JOptionPane.showConfirmDialog(MissionViewer.this, rangePanel,
                        "Set Range", JOptionPane.OK_CANCEL_OPTION);
                     if(result!=JOptionPane.CANCEL_OPTION)
                     {
                        try
                        {
                           iOffset = Integer.parseInt(offset.getText());
                           iLength = Integer.parseInt(length.getText());

                           dataPlot.setDisplayRange(iOffset, iLength);
                        }
                        catch(NumberFormatException nfe)
                        {
                        }
                     }
                  }
               }
            );*/
         buttonPanel.add(cmdButton[REFRESH_BTN]);
         buttonPanel.add(cmdButton[START_BTN]);
         buttonPanel.add(cmdButton[STOP_BTN]);
         //buttonPanel.add(rangeButton);
         JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         passwordPanel.setBackground(Color.lightGray);
            cmdButton[SFT_PSW_BTN] = new JButton("Set Container Password");
            cmdButton[SFT_PSW_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     byte[] readPassword = null, writePassword = null;
                     String strReadPassword
                        = JOptionPane.showInputDialog(MissionViewer.this,
                                                      "Read Password");
                     if(strReadPassword!=null && strReadPassword.length()>0)
                     {
                        try
                        {
                           readPassword = Convert.toByteArray(strReadPassword);
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        if(readPassword == null || readPassword.length!=8)
                        {
                           JOptionPane.showMessageDialog(MissionViewer.this,
                                                         "Bad password string, aborting.");
                           return;
                        }
                     }
                     String strWritePassword
                        = JOptionPane.showInputDialog(MissionViewer.this,
                                                      "Read/Write Password");
                     if(strWritePassword!=null && strWritePassword.length()>0)
                     {
                        try
                        {
                           writePassword = Convert.toByteArray(strWritePassword);
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        if(writePassword.length!=8)
                        {
                           JOptionPane.showMessageDialog(MissionViewer.this,
                                                         "Bad password string, aborting.");
                           return;
                        }
                     }
                     synchronized(syncObj)
                     {
                        OneWireContainer41 owc41 = (OneWireContainer41)container;
                        if(owc41==null)
                           return;
                        try
                        {
                           if(readPassword!=null)
                              owc41.setContainerReadOnlyPassword(readPassword, 0);
                           if(writePassword!=null)
                              owc41.setContainerReadWritePassword(writePassword, 0);
                        }
                        catch(Exception e)
                        {;}
                     }
                  }
               }
            );
            cmdButton[DEV_PSW_BTN] = new JButton("Set Device Password");
            cmdButton[DEV_PSW_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     byte[] readPassword = null, writePassword = null;
                     String strReadPassword
                        = JOptionPane.showInputDialog(MissionViewer.this,
                                                      "Read Password");
                     if(strReadPassword!=null && strReadPassword.length()>0)
                     {
                        try
                        {
                           readPassword = Convert.toByteArray(strReadPassword);
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        if(readPassword.length!=8)
                        {
                           JOptionPane.showMessageDialog(MissionViewer.this,
                                                         "Bad password string, aborting.");
                           return;
                        }
                     }
                     String strWritePassword
                        = JOptionPane.showInputDialog(MissionViewer.this,
                                                      "Read/Write Password");
                     if(strWritePassword!=null && strWritePassword.length()>0)
                     {
                        try
                        {
                           writePassword = Convert.toByteArray(strWritePassword);
                        }
                        catch(com.dalsemi.onewire.utils.Convert.ConvertException ce)
                        {
                           setStatus(ERRMSG, "Error during hex string conversion: " + ce);
                        }
                        if(writePassword.length!=8)
                        {
                           JOptionPane.showMessageDialog(MissionViewer.this,
                                                         "Bad password string, aborting.");
                           return;
                        }
                     }
                     synchronized(syncObj)
                     {
                        for(int btn=0; btn<TOTAL_BUTTONS; btn++)
                           cmdButton[btn].setEnabled(false);
                        SetDevicePasswordTask sdpt
                           = new SetDevicePasswordTask(
                              adapter, (OneWireContainer41)container,
                              readPassword, 0, writePassword, 0);
                        enqueueRunTask(sdpt);
                     }
                  }
               }
            );
            cmdButton[EN_PSW_BTN] = new JButton("Set Password Enable");
            cmdButton[EN_PSW_BTN].addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     int i = JOptionPane.showConfirmDialog(MissionViewer.this,
                         "Click 'Yes' to Enable Passwords, 'No' to Disable Passwords");
                     if(i==JOptionPane.YES_OPTION || i==JOptionPane.NO_OPTION)
                     {
                        synchronized(syncObj)
                        {
                           for(int btn=0; btn<TOTAL_BUTTONS; btn++)
                              cmdButton[btn].setEnabled(false);
                           SetPasswordEnableTask spet
                              = new SetPasswordEnableTask(
                                 adapter, (OneWireContainer41)container,
                                 i==JOptionPane.YES_OPTION);
                           enqueueRunTask(spet);
                        }
                     }
                  }
               }
            );
         passwordPanel.add(cmdButton[SFT_PSW_BTN]);
         passwordPanel.add(cmdButton[DEV_PSW_BTN]);
         passwordPanel.add(cmdButton[EN_PSW_BTN]);
         JPanel checkboxPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         checkboxPanel.setBackground(Color.lightGray);
            ButtonGroup group = new ButtonGroup();
            JCheckBox fahCheck = new JCheckBox("Fahrenheit", bFahrenheit);
            fahCheck.setBackground(Color.lightGray);
            fahCheck.setFont(fontPlain);
            group.add(fahCheck);
            fahCheck.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     if(container!=null)
                     {
                        bFahrenheit = true;
                        ViewerProperties.setPropertyBoolean(
                           MISSION_VIEWER_FAHRENHEIT, true);
                        enqueueRunTask(setupViewerTask);
                     }
                  }
               }
            );
            JCheckBox celCheck = new JCheckBox("Celsius", !bFahrenheit);
            celCheck.setBackground(Color.lightGray);
            celCheck.setFont(fontPlain);
            group.add(celCheck);
            celCheck.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     if(container!=null)
                     {
                        bFahrenheit = false;
                        ViewerProperties.setPropertyBoolean(
                           MISSION_VIEWER_FAHRENHEIT, false);
                        enqueueRunTask(setupViewerTask);
                     }
                  }
               }
            );
         checkboxPanel.add(fahCheck);
         checkboxPanel.add(celCheck);
      commandPanel.add(buttonPanel);
      commandPanel.add(passwordPanel);
      commandPanel.add(checkboxPanel);

      // add components to viewer
      add(commandScroll, BorderLayout.NORTH);
      add(tabbedResultsPane, BorderLayout.CENTER);

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
      return (owc instanceof MissionContainer);
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
            try
            {
               for(int btn=0; btn<TOTAL_BUTTONS; btn++)
                  cmdButton[btn].setEnabled(false);
               this.adapter = owc.getAdapter();
               this.container = (MissionContainer)owc;
               this.romID = owc.getAddressAsString();

               this.tabbedResultsPane.removeAll();
               tabbedResultsPane.addTab("Status",null,featureScroll,
                  "Mission Status");
               tabbedResultsPane.addTab("Temperature",null,temperatureScroll,
                  "Graphs the mission's temperature log");
               this.tabbedResultsPane.setEnabledAt(1, false);

               this.channelLabel = new String[this.container.getNumberMissionChannels()];
               this.hasResolution = new boolean[this.channelLabel.length];
               this.channelResolution = new double[this.channelLabel.length][];
               for(int i=0; i<this.channelLabel.length; i++)
               {
                  this.channelLabel[i] = this.container.getMissionLabel(i);
                  this.channelResolution[i] = this.container.getMissionResolutions(i);
                  this.hasResolution[i] = this.channelResolution[i].length>1;
               }
               this.lblFeature[MissionViewer.DATA_LOGGING].setVisible(
                  channelLabel.length==2);
               this.lblFeatureHdr[MissionViewer.DATA_LOGGING].setVisible(
                  channelLabel.length==2);
               this.lblFeature[MissionViewer.DATA_LOW_ALARM].setVisible(
                  channelLabel.length==2);
               this.lblFeatureHdr[MissionViewer.DATA_LOW_ALARM].setVisible(
                  channelLabel.length==2);
               this.lblFeature[MissionViewer.DATA_HIGH_ALARM].setVisible(
                  channelLabel.length==2);
               this.lblFeatureHdr[MissionViewer.DATA_HIGH_ALARM].setVisible(
                  channelLabel.length==2);
               if(channelLabel.length==2)
               {
                  tabbedResultsPane.addTab(channelLabel[1],null,dataScroll,
                     "Graphs the mission's data log");
                  this.tabbedResultsPane.setEnabledAt(2, false);
               }

            }
            catch(Exception e)
            {
               e.printStackTrace();
               setStatus(ERRMSG, "Error getting channel descriptions: " + e.getMessage());
            }
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
      try
      {
         setStatus(VERBOSE, "No Device");
         this.dataPlot.resetPlot();
         this.temperaturePlot.resetPlot();
         for(int i=0; i<TOTAL_FEATURES; i++)
            lblFeature[i].setText("");
      }
      catch(Exception e)
      {
         e.printStackTrace();
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
          && ( (runList.size()>1) || (runList.indexOf(setupViewerTask)<0) );
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

      //enqueuePollTask(pollTemperatureTask);
      //enqueuePollTask(pollResolutionTask);

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
      ThermochronViewer tv = new ThermochronViewer();
      tv.setContainer((OneWireContainer)this.container);
      return tv;
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * NewMissionTask encapsulates the action of initializing a new mission for
    * a Thermochron Device.
    */
   protected class DelayedInitMissionTask
      extends ViewerTask
      implements ActionListener, Runnable
   {
      InitMissionTask task_imt;
      JDialog dialog;
      boolean cancelled = false;
      public DelayedInitMissionTask(InitMissionTask imt)
      {
         this.task_imt = imt;
         JFrame frame = (JFrame)JOptionPane.getFrameForComponent(MissionViewer.this);
         this.dialog = new JDialog(frame, "Waiting");
         Point pFrame = frame.getLocation();
         pFrame.x += frame.getSize().width/2;
         pFrame.y += frame.getSize().height/2;
         this.dialog.setLocation(pFrame);
         this.dialog.setModal(true);
         dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

         JButton cancelButton = new JButton("Cancel 1-Second Mission Test");

         cancelButton.addActionListener(this);

         dialog.getContentPane().setLayout(new FlowLayout(FlowLayout.CENTER, 20, 15));
         dialog.getContentPane().add(cancelButton);
         dialog.pack();

      }

      public void actionPerformed(ActionEvent ae)
      {
         cancelled = true;
         dialog.setVisible(false);
      }

      public void run()
      {
         dialog.setVisible(true);
      }

      public void executeTask()
      {
         if(task_imt.task_adapter==null || task_imt.task_container==null)
            return;
         try
         {
            task_imt.task_adapter.beginExclusive(true);

            boolean  missionActive = task_imt.task_container.isMissionRunning();

            // disable current mission
            if(missionActive)
               task_imt.task_container.stopMission();

            // clear memory contents
            task_imt.task_container.clearMissionResults();
            ((OneWireContainer41)task_imt.task_container)
               .setStartUponTemperatureAlarmEnable(false);
            task_imt.task_container.setMissionResolution(0,
                     task_imt.task_container.getMissionResolutions(0)[0]);
            task_imt.task_container.startNewMission(1, 0,
                     false, true, new boolean[]{true, false});

            setStatus(MESSAGE, " New 1-Second Mission Initialized!");

            Thread t = new Thread(this);
            t.start();

            int retryCnt = 5;
            int missionSampleCount = 0;
            while(retryCnt-->0 && missionSampleCount==0 && !cancelled)
            {
               try
               {
                  missionSampleCount =
                     ((OneWireContainer41)task_imt.task_container).readByte(
                        OneWireContainer41.MISSION_SAMPLE_COUNT);
                  retryCnt = 5;
               }
               catch(Exception e)
               {
                  missionSampleCount = 0;
               }
               setStatus(VERBOSE, " Polling 1-Second Mission Start Time");
               Thread.sleep(500);
            }
            task_imt.task_container.stopMission();
            if(cancelled)
            {
              setStatus(MESSAGE, " CANCELLED");
            }
            else
            {
               setStatus(MESSAGE, " New 1-Second Mission Completed!");
               enqueueRunTask(task_imt);
            }
            enqueueRunTask(setupViewerTask);
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Delayed Initialize Mission error: " + e.toString());
         }
         finally
         {
            task_imt.task_adapter.endExclusive();
            dialog.setVisible(false);
            dialog.dispose();
         }

      }
   }
   /**
    * NewMissionTask encapsulates the action of initializing a new mission for
    * a Thermochron Device.
    */
   protected class InitMissionTask extends ViewerTask
   {
      DSPortAdapter task_adapter;
      MissionContainer task_container;
      boolean task_rollover, task_syncClock, task_suta;
      int task_rate, task_delay;
      boolean[] task_enableAlarm, task_channelEnabled;
      double[] task_low, task_high, task_resolution;

      public InitMissionTask(DSPortAdapter adapter, MissionContainer container,
         boolean[] channelEnabled, boolean rollover, boolean syncClock, boolean suta, int rate,
         int delay, boolean[] enableAlarm, double[] low, double[] high, double[] resolution)
      {
         task_adapter = adapter;
         task_container = container;
         task_channelEnabled = channelEnabled;
         task_rollover = rollover;
         task_syncClock = syncClock;
         task_suta = suta;
         task_rate = rate;
         task_delay = delay;
         task_enableAlarm = enableAlarm;
         task_low = low;
         task_high = high;
         task_resolution = resolution;
      }

      public void executeTask()
      {
         if(task_adapter==null || task_container==null)
            return;
         try
         {
            task_adapter.beginExclusive(true);

            boolean  missionActive = task_container.isMissionRunning();

            // disable current mission
            if(missionActive)
               task_container.stopMission();

            // clear memory contents
            task_container.clearMissionResults();

            boolean anyAlarmsEnabled = false;
            if(task_enableAlarm!=null && task_high!=null && task_low!=null)
            {
               for(int i=0; i<task_enableAlarm.length; i++)
               {
                  if(task_enableAlarm[i])
                  {
                     anyAlarmsEnabled = true;
                     task_container.setMissionAlarm(i,
                                     MissionContainer.ALARM_HIGH, task_high[i]);
                     task_container.setMissionAlarm(i,
                                     MissionContainer.ALARM_LOW, task_low[i]);
                  }
                  task_container.setMissionAlarmEnable(i,
                     MissionContainer.ALARM_HIGH, task_enableAlarm[i]);
                  task_container.setMissionAlarmEnable(i,
                     MissionContainer.ALARM_LOW, task_enableAlarm[i]);
                  task_container.setMissionResolution(i, task_resolution[i]);
               }
            }
            ((OneWireContainer41)task_container).setStartUponTemperatureAlarmEnable(task_suta && anyAlarmsEnabled);

            task_container.startNewMission(task_rate, task_delay,
                                           task_rollover, task_syncClock,
                                           task_channelEnabled);

            setStatus(MESSAGE, " New Mission Initialized!");
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Initialize Mission error: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * DisableMissionTask encapsulates the action of disabling a mission for
    * a Thermochron Device.
    */
   protected class DisableMissionTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      MissionContainer task_container = null;

      public DisableMissionTask(DSPortAdapter adapter, MissionContainer container)
      {
         task_adapter = adapter;
         task_container = container;
      }

      public void executeTask()
      {
         if(task_adapter==null || task_container==null)
            return;
         try
         {
            task_adapter.beginExclusive(true);
            boolean  missionActive = task_container.isMissionRunning();

            // disable current mission
            if(missionActive)
            {
               task_container.stopMission();
               setStatus(MESSAGE, " Mission Disabled!");
            }
            else
               setStatus(MESSAGE, " No Mission in Progress!");
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Initialize Mission error: " + e.toString());
         }
         finally
         {
            task_adapter.endExclusive();
         }
      }
   }

   /**
    * SetupViewerTask encapsulates the action of setting up the viewer for
    * a Thermochron Device.  Since this class is, essentially,
    * parameter-less (or, rather it grabs its parameters from the current state
    * of the viewer) only one instance is really necessary.
    */
   protected class SetupViewerTask extends ViewerTask
   {
      public boolean reloadResults = false;

      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         MissionContainer l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         MissionViewer.this.tabbedResultsPane.setSelectedIndex(0);
         MissionViewer.this.tabbedResultsPane.setEnabledAt(1, false);
         if(MissionViewer.this.tabbedResultsPane.getTabCount()==3)
            MissionViewer.this.tabbedResultsPane.setEnabledAt(2, false);
         setStatus(VERBOSE, "Setting up viewer");
         try
         {
            l_adapter.beginExclusive(true);
            boolean success = true;
            if(reloadResults || !l_container.isMissionLoaded())
            {
               success = false;
               int retryCount = 5;
               while(!success && retryCount>0)
               {
                  try
                  {
                     l_container.loadMissionResults();
                     success = true;
                  }
                  catch(Exception e)
                  {
                     retryCount--;
                     e.printStackTrace();
                  }
               }
            }
            reloadResults = !success;

            boolean  missionActive = l_container.isMissionRunning();
            int sample_rate = l_container.getMissionSampleRate(0);
            lblFeature[IS_ACTIVE].setText(" " + missionActive);
            lblFeature[MISSION_SUTA].setText(" "
               + ((OneWireContainer41)l_container).isMissionSUTA());
            lblFeature[MISSION_WFTA].setText(" "
               + ((OneWireContainer41)l_container).isMissionWFTA());
            lblFeature[SAMPLE_RATE].setText(" Every " +
                           (sample_rate) + " second(s)");
            int sample_count = l_container.getMissionSampleCount(0)
                             | l_container.getMissionSampleCount(1);
            if(sample_count>0)
            {
               lblFeature[MISSION_START].setText(" " +
                              new Date(l_container.getMissionTimeStamp(0)));
            }
            else
            {
               lblFeature[MISSION_START].setText(" First sample not yet logged");

            }
            lblFeature[MISSION_SAMPLES].setText(" " +
                           nf.format(sample_count));
            lblFeature[ROLL_OVER].setText(" " + l_container.isMissionRolloverEnabled()
             + (l_container.hasMissionRolloverOccurred()?"(rolled over)":"(no rollover)"));
            lblFeature[FIRST_SAMPLE_TIMESTAMP].setText(" " + new Date(
               l_container.getMissionSampleTimeStamp(0,0) | l_container.getMissionSampleTimeStamp(1,0)));
            lblFeature[TOTAL_SAMPLES].setText(" " + l_container.getMissionSampleCountTotal(0));

            if(l_container.getMissionChannelEnable(0))
               lblFeature[TEMP_LOGGING].setText(" " + l_container.getMissionResolution(0) + " bit");
            else
               lblFeature[TEMP_LOGGING].setText(" disabled");

            if(l_container.getMissionChannelEnable(0) &&
               l_container.getMissionAlarmEnable(0, MissionContainer.ALARM_HIGH))
            {
               if(bFahrenheit)
               {
                  // read the high temperature alarm setting
                  String highAlarmText = nf.format(
                     Convert.toFahrenheit(
                        l_container.getMissionAlarm(0,MissionContainer.ALARM_HIGH)));
                  lblFeature[TEMP_HIGH_ALARM].setText(" " + highAlarmText + " °F");
               }
               else
               {
                  // read the high temperature alarm setting
                  String highAlarmText = nf.format(
                        l_container.getMissionAlarm(0,MissionContainer.ALARM_HIGH));
                  lblFeature[TEMP_HIGH_ALARM].setText(" " + highAlarmText + " °C");
               }
               if(l_container.hasMissionAlarmed(0, MissionContainer.ALARM_LOW))
                  lblFeature[TEMP_HIGH_ALARM].setText(
                           lblFeature[TEMP_HIGH_ALARM].getText() + " (ALARMED)");
            }
            else
            {
               lblFeature[TEMP_HIGH_ALARM].setText(" disabled");
            }
            if(l_container.getMissionChannelEnable(0) &&
               l_container.getMissionAlarmEnable(0, MissionContainer.ALARM_LOW))
            {
               if(bFahrenheit)
               {
                  // read the low temperature alarm setting
                  String lowAlarmText = nf.format(
                     Convert.toFahrenheit(
                        l_container.getMissionAlarm(0,MissionContainer.ALARM_LOW)));
                  lblFeature[TEMP_LOW_ALARM].setText(" " + lowAlarmText + " °F");
               }
               else
               {
                  // read the low temperature alarm setting
                  String lowAlarmText = nf.format(
                        l_container.getMissionAlarm(0,MissionContainer.ALARM_LOW));
                  lblFeature[TEMP_LOW_ALARM].setText(" " + lowAlarmText + " °C");
               }
               if(l_container.hasMissionAlarmed(0, MissionContainer.ALARM_LOW))
                  lblFeature[TEMP_LOW_ALARM].setText(
                           lblFeature[TEMP_LOW_ALARM].getText() + " (ALARMED)");
            }
            else
            {
               lblFeature[TEMP_LOW_ALARM].setText(" disabled");
            }

            if(l_container.getMissionChannelEnable(1))
               lblFeature[DATA_LOGGING].setText(" " + l_container.getMissionResolution(1) + " bit");
            else
               lblFeature[DATA_LOGGING].setText(" disabled");
            if(l_container.getMissionChannelEnable(1) &&
               l_container.getMissionAlarmEnable(1, MissionContainer.ALARM_HIGH))
            {
               String highAlarmText = nf.format(
                  l_container.getMissionAlarm(1,MissionContainer.ALARM_HIGH));
               if(l_container.getMissionLabel(1).equals("Humidity"))
                  lblFeature[DATA_HIGH_ALARM].setText(" " + highAlarmText + " %RH");
               else if(l_container.getMissionLabel(1).equals("Voltage"))
                  lblFeature[DATA_HIGH_ALARM].setText(" " + highAlarmText + " V");
               else
                  lblFeature[DATA_HIGH_ALARM].setText(" " + highAlarmText);
               if(l_container.hasMissionAlarmed(1, MissionContainer.ALARM_HIGH))
                  lblFeature[DATA_HIGH_ALARM].setText(
                           lblFeature[DATA_HIGH_ALARM].getText() + " (ALARMED)");
            }
            else
            {
               lblFeature[DATA_HIGH_ALARM].setText(" disabled");
            }
            if(l_container.getMissionChannelEnable(1) &&
               l_container.getMissionAlarmEnable(1, MissionContainer.ALARM_LOW))
            {
               String lowAlarmText = nf.format(
                  l_container.getMissionAlarm(1,MissionContainer.ALARM_LOW));
               if(l_container.getMissionLabel(1).equals("Humidity"))
                  lblFeature[DATA_LOW_ALARM].setText(" " + lowAlarmText + " %RH");
               else if(l_container.getMissionLabel(1).equals("Voltage"))
                  lblFeature[DATA_LOW_ALARM].setText(" " + lowAlarmText + " V");
               else
                  lblFeature[DATA_LOW_ALARM].setText(" " + lowAlarmText);
               if(l_container.hasMissionAlarmed(1, MissionContainer.ALARM_LOW))
                  lblFeature[DATA_LOW_ALARM].setText(
                           lblFeature[DATA_LOW_ALARM].getText() + " (ALARMED)");
            }
            else
            {
               lblFeature[DATA_LOW_ALARM].setText(" disabled");
            }

            lblFeature[DEVICE_SAMPLES].setText(" "
               + ((OneWireContainer41)l_container).getDeviceSampleCount());

            String useTempCal
               = com.dalsemi.onewire.OneWireAccessProvider.getProperty(
                  "DS1922H.useTemperatureCalibrationRegisters");
            if(useTempCal!=null)
            {
               ((OneWireContainer41)l_container).setTemperatureCalibrationRegisterUsage(
                  !useTempCal.toLowerCase().equals("false"));
            }
            else
            {
               ((OneWireContainer41)l_container).setTemperatureCalibrationRegisterUsage(
                  ViewerProperties.getPropertyBoolean("DS1922H.useTemperatureCalibrationRegisters", true));
            }

            temperaturePlot.resetPlot();
            if(l_container.getMissionChannelEnable(0))
            {
               if(bFahrenheit)
               {
                  // plot the temperature log
                  for(int i=0; i<l_container.getMissionSampleCount(0); i++)
                     temperaturePlot.addPoint(
                        Convert.toFahrenheit(
                           l_container.getMissionSample(0,i)),
                        df.format(
                           new Date(
                              l_container.getMissionSampleTimeStamp(0, i)))
                        + ",F");
               }
               else
               {
                  // plot the temperature log
                  for(int i=0; i<l_container.getMissionSampleCount(0); i++)
                     temperaturePlot.addPoint(
                        l_container.getMissionSample(0,i),
                        df.format(
                           new Date(
                              l_container.getMissionSampleTimeStamp(0, i)))
                        + ",C");
               }
               MissionViewer.this.tabbedResultsPane.setEnabledAt(1, true);
               //if(l_container.getMissionSampleCount(0)>2048)
               //   temperaturePlot.zoomFull();
            }

            /*String useHumdCal
               = com.dalsemi.onewire.OneWireAccessProvider.getProperty(
                  "DS1922H.useHumidityCalibrationRegisters");
            if(useHumdCal!=null)
            {
               ((OneWireContainer41)l_container).setHumidityCalibrationRegisterUsage(
                  !useHumdCal.toLowerCase().equals("false"));
            }
            else
            {
               ((OneWireContainer41)l_container).setHumidityCalibrationRegisterUsage(
                  ViewerProperties.getPropertyBoolean("DS1922H.useHumidityCalibrationRegisters", true));
            }*/

            dataPlot.resetPlot();
            if(l_container.getMissionChannelEnable(1))
            {
               if(l_container.getMissionLabel(1).equals("Data"))
               {
                  // plot the data log
                  for(int i=0; i<l_container.getMissionSampleCount(1); i++)
                     dataPlot.addPoint(
                        l_container.getMissionSampleAsInteger(1,i),
                           df.format(
                              new Date(
                                 l_container.getMissionSampleTimeStamp(0, i)))

                           + ",Data");
               }
               else
               {
                  // plot the data log
                  for(int i=0; i<l_container.getMissionSampleCount(1); i++)
                     dataPlot.addPoint(
                        l_container.getMissionSample(1,i),
                           df.format(
                              new Date(
                                 l_container.getMissionSampleTimeStamp(0, i)))

                           + ",%RH");

               }
               MissionViewer.this.tabbedResultsPane.setEnabledAt(2, true);
               //if(l_container.getMissionSampleCount(0)>2048)
               //   dataPlot.zoomFull();
            }

            // Read the alarm history
            /*
            StringBuffer alarmText = new StringBuffer();
            byte[] low_history = l_container.getAlarmHistory(l_container.TEMPERATURE_LOW_ALARM);
            if (low_history.length == 0)
            {
               alarmText.append(
                  "- No violations against the low temperature alarm.");
               alarmText.append("\n");
            }
            else
               for (int i = 0; i < low_history.length / 4; i++)
               {
                  int start_offset  = (low_history [i * 4] & 0x0ff)
                                    | ((low_history [i * 4 + 1] << 8) & 0x0ff00)
                                    | ((low_history [i * 4 + 2] << 16) & 0x0ff0000);
                  int violation_count = 0x0ff & low_history [i * 4 + 3];

                  alarmText.append("- Low alarm started at     : ");
                  alarmText.append(start_offset * sample_rate);
                  alarmText.append("\n");
                  alarmText.append("-                          : Lasted ");
                  alarmText.append(violation_count * sample_rate);
                  alarmText.append(" minutes");
                  alarmText.append("\n");
               }

            byte[] high_history = l_container.getAlarmHistory(l_container.TEMPERATURE_HIGH_ALARM);
            if (high_history.length == 0)
            {
               alarmText.append(
                  "- No violations against the high temperature alarm.");
               alarmText.append("\n");
            }
            else
               for (int i = 0; i < high_history.length / 4; i++)
               {
                  int start_offset  = (high_history [i * 4] & 0x0ff)
                                    | ((high_history [i * 4 + 1] << 8) & 0x0ff00)
                                    | ((high_history [i * 4 + 2] << 16)
                                       & 0x0ff0000);
                  int violation_count = 0x0ff & high_history [i * 4 + 3];

                  alarmText.append("- High alarm started at    : ");
                  alarmText.append(start_offset * sample_rate);
                  alarmText.append("\n");
                  alarmText.append("-                          : Lasted ");
                  alarmText.append(violation_count * sample_rate);
                  alarmText.append(" minutes");
                  alarmText.append("\n");
               }
            alarmHistory.setText(alarmText.toString());

            */

            // Temperature Histogram
            /*
            StringBuffer histogramText = new StringBuffer();
            double resolution = l_container.getTemperatureResolution();
            double histBinWidth = l_container.getHistogramBinWidth();
            double start = l_container.getHistogramLowTemperature();
            int[] histogramArray = l_container.getTemperatureHistogram();

            for (int i = 0; i < histogramArray.length; i++)
            {
               histogramText.append("- Histogram entry          : ");
               histogramText.append(histogramArray[i]);
               histogramText.append(" at temperature ");
               if(bFahrenheit)
               {
                  histogramText.append(nf.format(Convert.toFahrenheit(start)));
                  histogramText.append(" to ");
                  histogramText.append(nf.format(Convert.toFahrenheit(start +
                                                 (histBinWidth - resolution))));
                  histogramText.append(" °F");
               }
               else
               {
                  histogramText.append(nf.format(start));
                  histogramText.append(" to ");
                  histogramText.append(nf.format(start + (histBinWidth - resolution)));
                  histogramText.append(" °C");
               }
               histogramText.append("\n");

               start += histBinWidth;
            }
            histogram.setText(histogramText.toString());
            */
         }
         catch(Exception e)
         {
            e.printStackTrace();
            setStatus(ERRMSG, "Setup Error: " + e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
         setStatus(VERBOSE, "Done Setting up viewer");
         for(int btn=0; btn<TOTAL_BUTTONS; btn++)
            cmdButton[btn].setEnabled(true);
      }
   }

   /**
    * Panel for querying all new mission parameters
    */
   class InitMissionPanel extends JPanel
   {
      JCheckBox[] chkChannelEnabled, chkAlarmEnabled;
      JCheckBox chkSyncClock, chkRollover, chkSUTA;
      JTextField txtSampleRate,txtStartDelay;
      JTextField[] txtLowAlarm, txtHighAlarm;
      JList[] lstResolution;
      JCheckBox oneSecMissionTest;

      public InitMissionPanel(String[] channelLabels,
                              boolean[] hasResolution,
                              double[][] resolutions)
      {
         super(new BorderLayout(3,3));

         chkChannelEnabled = new JCheckBox[channelLabels.length];
         chkAlarmEnabled = new JCheckBox[channelLabels.length];
         txtLowAlarm = new JTextField[channelLabels.length];
         txtHighAlarm = new JTextField[channelLabels.length];
         lstResolution = new JList[channelLabels.length];

         JPanel tempGrid = new JPanel(new GridLayout(3,2));
         tempGrid.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Mission General"));
         JPanel tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
         JPanel tempBox2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            // synchronize clock checkbox
            chkSyncClock = new JCheckBox("Synchronize Clock? ");
            chkSyncClock.setSelected(true);
            chkSyncClock.setFont(fontBold);
            chkSyncClock.setHorizontalAlignment(SwingConstants.CENTER);
            // enable rollover checkbox
            chkRollover = new JCheckBox("Enable Rollover?");
            chkRollover.setSelected(true);
            chkRollover.setFont(fontBold);
            chkRollover.setHorizontalAlignment(SwingConstants.CENTER);
            // sample rate input
            JLabel lblTemp = new JLabel("Sampling Rate (seconds) ");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox1.add(lblTemp);
            txtSampleRate = new JTextField(6);
            txtSampleRate.setFont(fontPlain);
            txtSampleRate.setText("600");
            tempBox1.add(txtSampleRate);
            // start delay input
            lblTemp = new JLabel("Start Delay (minutes) ");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox2.add(lblTemp);
            txtStartDelay = new JTextField(6);
            txtStartDelay.setFont(fontPlain);
            txtStartDelay.setText("0");
            tempBox2.add(txtStartDelay);

            oneSecMissionTest = new JCheckBox("Use 1-Second Mission Test? ");
            oneSecMissionTest.setFont(fontBold);
            oneSecMissionTest.setHorizontalAlignment(SwingConstants.CENTER);
            oneSecMissionTest.setForeground(Color.blue);
            oneSecMissionTest.setToolTipText(
                     "Starts 1-Second Mission first, then re-starts with correct parameters after first sample");
         tempGrid.add(chkSyncClock);
         tempGrid.add(tempBox1);
         tempGrid.add(chkRollover);
         tempGrid.add(tempBox2);
         tempGrid.add(oneSecMissionTest);

         add(tempGrid, BorderLayout.NORTH);

         tempGrid = new JPanel(new GridLayout(channelLabels.length,1));
         for(int i=0; i<channelLabels.length; i++)
         {
            boolean tempChan = channelLabels[i].equals("Temperature");
            JPanel tempPanel = new JPanel(new BorderLayout());
            tempPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Mission Channel: " + channelLabels[i]));
            JPanel leftPanel = new JPanel(new GridLayout(2, 1));
            JPanel rightPanel = new JPanel(new GridLayout(tempChan?4:3, 1));
            tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            tempBox2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JPanel tempBox3 = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JPanel tempBox4 = tempChan?new JPanel(new FlowLayout(FlowLayout.CENTER)):null;
               // enable channel checkbox
               chkChannelEnabled[i] = new JCheckBox("Enable Sampling? ");
               chkChannelEnabled[i].setSelected(true);
               chkChannelEnabled[i].setFont(fontBold);
               chkChannelEnabled[i].setHorizontalAlignment(SwingConstants.CENTER);
               // Select resolution
               lblTemp = new JLabel("Resolution: ");
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox3.add(lblTemp);
               if(hasResolution[i])
               {
                  Vector v = new Vector(resolutions[i].length);
                  for(int j=0; j<resolutions[i].length; j++)
                     v.addElement(new Double(resolutions[i][j]));
                  lstResolution[i] = new JList(v);
                  lstResolution[i].setFont(fontBold);
                  lstResolution[i].setSelectedIndex(0);
                  tempBox3.add(lstResolution[i]);
               }
               else
               {
                  lstResolution[i] = new JList(new String[]{"N/A"});
                  lstResolution[i].setVisibleRowCount(1);
                  lstResolution[i].setFont(fontBold);
               }
               // enable alarm checkbox
               chkAlarmEnabled[i] = new JCheckBox("Enable Alarms?");
               chkAlarmEnabled[i].setSelected(false);
               chkAlarmEnabled[i].setFont(fontBold);
               chkAlarmEnabled[i].setHorizontalAlignment(SwingConstants.CENTER);
               // low alarm input
               if(channelLabels[i].equals("Temperature"))
               {
                  if(bFahrenheit)
                     lblTemp = new JLabel("Low Alarm? (°F)");
                  else
                     lblTemp = new JLabel("Low Alarm? (°C)");
               }
               else if(channelLabels[i].equals("Humidity"))
               {
                  lblTemp = new JLabel("Low Alarm? (%RH)");
               }
               else
               {
                  lblTemp = new JLabel("Low Alarm?");
               }
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox1.add(lblTemp);
               txtLowAlarm[i] = new JTextField(6);
               txtLowAlarm[i].setFont(fontPlain);
               tempBox1.add(txtLowAlarm[i]);
               // high alarm input
               if(tempChan)
               {
                  if(bFahrenheit)
                     lblTemp = new JLabel("High Alarm? (°F)");
                  else
                     lblTemp = new JLabel("High Alarm? (°C)");
               }
               else if(channelLabels[i].equals("Humidity"))
               {
                  lblTemp = new JLabel("High Alarm? (%RH)");
               }
               else
               {
                  lblTemp = new JLabel("High Alarm?");
               }
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox2.add(lblTemp);
               txtHighAlarm[i] = new JTextField(6);
               txtHighAlarm[i].setFont(fontPlain);
               tempBox2.add(txtHighAlarm[i]);
               if(tempChan)
               {
                  chkSUTA = new JCheckBox("Enable SUTA?");
                  tempBox4.add(chkSUTA);
               }
            leftPanel.add(chkChannelEnabled[i]);
            leftPanel.add(tempBox3);
            rightPanel.add(chkAlarmEnabled[i]);
            rightPanel.add(tempBox1);
            rightPanel.add(tempBox2);
            if(tempChan)
               rightPanel.add(tempBox4);
            tempPanel.add(leftPanel, BorderLayout.CENTER);
            tempPanel.add(rightPanel, BorderLayout.EAST);

            tempGrid.add(tempPanel);
         }

         add(tempGrid, BorderLayout.WEST);
      }
   }

   /**
    */
   protected class SetDevicePasswordTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer41 task_container = null;
      byte[] task_readPassword = null;
      int task_readOffset;
      byte[] task_writePassword = null;
      int task_writeOffset;

      public SetDevicePasswordTask(DSPortAdapter adapter,
                                   OneWireContainer41 container,
                                   byte[] readPassword, int readOffset,
                                   byte[] writePassword, int writeOffset
                                   )
      {
         task_adapter = adapter;
         task_container = container;
         task_readPassword = readPassword;
         task_readOffset = readOffset;
         task_writePassword = writePassword;
         task_writeOffset = writeOffset;
      }

      public void executeTask()
      {
         if(task_adapter==null || task_container==null)
            return;
         try
         {
            task_adapter.beginExclusive(true);
            if(task_container.isMissionRunning())
            {
               setStatus(ERRMSG, "Cannot change password while mission is in progress");
            }
            else
            {
               if(task_writePassword!=null)
               {
                  setStatus(VERBOSE, "Setting Read/Write Password");
                  task_container.setDeviceReadWritePassword(task_writePassword, task_writeOffset);
               }
               if(task_readPassword!=null)
               {
                  setStatus(VERBOSE, "Setting Read Password");
                  task_container.setDeviceReadOnlyPassword(task_readPassword, task_readOffset);
               }
               setStatus(VERBOSE, "Done Setting Passwords!");
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Set Password error: " + e.toString());
            e.printStackTrace();
         }
         finally
         {
            task_adapter.endExclusive();
         }
         for(int btn=0; btn<TOTAL_BUTTONS; btn++)
            cmdButton[btn].setEnabled(true);
      }
   }
   /**
    */
   protected class SetPasswordEnableTask extends ViewerTask
   {
      DSPortAdapter task_adapter = null;
      OneWireContainer41 task_container = null;
      boolean task_passwordsEnabled;
      byte[] task_readPassword = null;
      int task_readOffset;
      byte[] task_writePassword = null;
      int task_writeOffset;

      public SetPasswordEnableTask(DSPortAdapter adapter,
                                   OneWireContainer41 container,
                                   boolean passwordsEnabled)
      {
         task_adapter = adapter;
         task_container = container;
         task_passwordsEnabled = passwordsEnabled;
      }

      public void executeTask()
      {
         if(task_adapter==null || task_container==null)
            return;
         try
         {
            setStatus(VERBOSE, "Setting Password Enable to " + task_passwordsEnabled);
            task_adapter.beginExclusive(true);
            if(task_container.isMissionRunning())
            {
               setStatus(ERRMSG, "Cannot change password while mission is in progress");
            }
            else
            {
               task_container.setDevicePasswordEnableAll(task_passwordsEnabled);
               setStatus(VERBOSE, "Done! Password Enable set to " + task_passwordsEnabled);
            }
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Set Password Enable error: " + e.toString());
            e.printStackTrace();
         }
         finally
         {
            task_adapter.endExclusive();
         }
         for(int btn=0; btn<TOTAL_BUTTONS; btn++)
            cmdButton[btn].setEnabled(true);
      }
   }
}