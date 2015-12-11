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
import com.dalsemi.onewire.container.OneWireContainer21;
import com.dalsemi.onewire.container.TemperatureContainer;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.utils.Convert;

/**
 * A Thermochron (DS1921) Viewer for integration in the OneWireViewer.
 * All Thermochron (DS1921) devices are supported by this viewer.  This
 * viewer displays the current mission information as well as the temperature
 * log, the histogram, and the alarm log.
 *
 * @author SH
 * @version 1.00
 */
public class ThermochronViewer extends Viewer
   implements Pollable, Runnable
{
   /* string constants */
   private static final String strTitle = "ThermochronViewer";
   private static final String strTab = "Thermochron";
   private static final String strTip = "Inits Thermochron's mission or displays mission status/results";
   private static final String naString = "N/A";

   public static final String THERMOCHRON_VIEWER_FAHRENHEIT =
      "thermochron.viewer.displayFahrenheit";

   /* container variables */
   private OneWireContainer21 container = null;
   private TaggedDevice taggedDevice = null;

   /* visual components */
   private Plot temperaturePlot = null;
   private JTabbedPane tabbedResultsPane = null;
   private JTextArea alarmHistory = null;
   private JTextArea histogram = null;

   /* feature labels */
   private JLabel[] lblFeature = null, lblFeatureHdr = null;
   private String[] strHeader = {
      "Is Mission Active? ", "Mission Start: ", "Sample Rate: ",
      "Number of Mission Samples: ", "Total Samples: ",
      "Roll Over Enabled? ", "Roll Over Occurred? ",
      "Active Alarms: ", "Next Clock Alarm At: ",
      "High Temperature Alarm: ", "Low Temperature Alarm: "
   };
   /* indices for feature labels */
   private static final int TOTAL_FEATURES = 11;
   private static final int IS_ACTIVE = 0;
   private static final int MISSION_START = 1;
   private static final int SAMPLE_RATE = 2;
   private static final int MISSION_SAMPLES = 3;
   private static final int TOTAL_SAMPLES = 4;
   private static final int ROLL_OVER = 5;
   private static final int ROLLED_OVER = 6;
   private static final int ALARMS = 7;
   private static final int NEXT_CLOCK_ALARM = 8;
   private static final int HIGH_ALARM = 9;
   private static final int LOW_ALARM = 10;

   private volatile boolean pausePoll = false, pollRunning = false;
   private boolean bFahrenheit = true;
   private static final java.text.NumberFormat nf =
      new java.text.DecimalFormat();
   private static final java.text.DateFormat df =
      java.text.DateFormat.getDateTimeInstance(
         java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM);


   /* single-instance Viewer Tasks */
   //private final ViewerTask pollTemperatureTask = new PollTemperatureTask();
   //private final ViewerTask pollResolutionTask = new PollResolutionTask();
   private final ViewerTask setupViewerTask = new SetupViewerTask();

   public ThermochronViewer()
   {
      super(strTitle);

      bFahrenheit = ViewerProperties.getPropertyBoolean(
         THERMOCHRON_VIEWER_FAHRENHEIT, false);

      // set the version
      majorVersionNumber = 1;
      minorVersionNumber = 6;

      nf.setMaximumFractionDigits(3);
      nf.setGroupingUsed(false);

      // tabbed results pane
      // This pane consists of the status panel, the temperature panel,
      // the histogram panel, and the alarm history panel.
      tabbedResultsPane = new JTabbedPane(SwingConstants.TOP);
         // feature panel
         JPanel featurePanel = new JPanel(new GridLayout(TOTAL_FEATURES, 2, 3, 3));
         JScrollPane featureScroll = new JScrollPane(featurePanel,
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
         JPanel temperaturePanel = new JPanel(new BorderLayout());
         JScrollPane temperatureScroll = new JScrollPane(temperaturePanel,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            temperaturePlot = new Plot();
            temperaturePlot.setScale(60, 85);
         temperaturePanel.add(temperaturePlot, BorderLayout.CENTER);
         // Histogram panel
         JPanel histogramPanel = new JPanel(new BorderLayout());
         JScrollPane histogramScroll = new JScrollPane(histogramPanel,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            histogram = new JTextArea();
            histogram.setEditable(false);
            histogram.setFont(fontPlain);
         histogramPanel.add(histogram, BorderLayout.CENTER);
         // Alarm History panel
         JPanel alarmHistoryPanel = new JPanel(new BorderLayout());
         JScrollPane alarmHistoryScroll = new JScrollPane(alarmHistoryPanel,
                                       JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                       JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            alarmHistory = new JTextArea();
            alarmHistory.setEditable(false);
            alarmHistory.setFont(fontPlain);
         alarmHistoryPanel.add(alarmHistory, BorderLayout.CENTER);
      // add above panels to the tabbedResultsPane
      tabbedResultsPane.addTab("Status",null,featureScroll, "Mission Status");
      tabbedResultsPane.addTab("Temperatures",null,temperatureScroll,"Graphs the mission's temperature log");
      tabbedResultsPane.addTab("Histogram",null,histogramScroll,"Shows the mission's temperature histogram");
      tabbedResultsPane.addTab("Alarm Log",null,alarmHistoryScroll, "Shows a mission's alarm history");

      // Refresh Panel for Refresh Button.
      JPanel commandPanel = new JPanel(new GridLayout(2,1,3,3));
      JScrollPane commandScroll = new JScrollPane(commandPanel);
      commandPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Command"));
         JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
         buttonPanel.setBackground(Color.lightGray);
            JButton refreshButton = new JButton("Refresh Mission Results");
            refreshButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     enqueueRunTask(setupViewerTask);
                  }
               }
            );
            JButton newMissionButton = new JButton("Start New Mission");
            newMissionButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     InitMissionPanel imp = new InitMissionPanel();
                     int result = JOptionPane.showConfirmDialog(ThermochronViewer.this, imp,
                        "Initialize New Mission", JOptionPane.OK_CANCEL_OPTION);
                     if(result!=JOptionPane.CANCEL_OPTION)
                     {
                        boolean rollover, syncClock, clockalarm;
                        int rate, delay;
                        double low=-40.0, high=85.0;
                        int frequency=0, second=0, minute=0, hour=0, day=1;

                        rollover = imp.chkRollover.isSelected();
                        syncClock = imp.chkSyncClock.isSelected();

                        try
                        {
                           rate = Integer.parseInt(imp.txtSampleRate.getText());
                           delay = Integer.parseInt(imp.txtStartDelay.getText());
                        }
                        catch(NumberFormatException nfe)
                        {
                           setStatus(ERRMSG, "Bad Number Format: " + nfe);
                           return;
                        }

                        try
                        {
                           low = Double.parseDouble(imp.txtLowAlarm.getText());
                        }
                        catch(NumberFormatException nfe)
                        {
                           low = InitLowTempTask(container);
                           setStatus(ERRMSG, "Bad Number Format: " + nfe + ", using low alarm default");
                        }

                        try
                        {
                           high = Double.parseDouble(imp.txtHighAlarm.getText());
                        }
                        catch(NumberFormatException nfe)
                        {
                           high = InitHighTempTask(container);
                           setStatus(ERRMSG, "Bad Number Format: " + nfe + ", using high alarm default");
                        }

                        if(bFahrenheit)
                        {
                           low = Convert.toCelsius(low);
                           high = Convert.toCelsius(high);
                        }

                        clockalarm = imp.chkClockAlarm.isSelected();
                        if(clockalarm)
                        {
                           for(int i = 0; i<imp.frequencyOptions.length; i++)
                              if(imp.frequencyOptions[i].isSelected())
                              {
                                 frequency = i;
                                 i = imp.frequencyOptions.length;
                              }

                           try
                           {
                              second = Integer.parseInt(imp.txtSecond.getText());
                              if(frequency > 0)
                              {
                                 minute = Integer.parseInt(imp.txtMinute.getText());
                                 if(frequency > 1)
                                 {
                                    hour = Integer.parseInt(imp.txtHour.getText());
                                    if(frequency > 2)
                                    {
                                       day = Integer.parseInt(imp.txtDay.getText());
                                       if(frequency > 3)
                                          frequency = OneWireContainer21.ONCE_PER_WEEK;
                                       else
                                          frequency = OneWireContainer21.ONCE_PER_DAY;
                                    }
                                    else
                                       frequency = OneWireContainer21.ONCE_PER_HOUR;
                                 }
                                 else
                                    frequency = OneWireContainer21.ONCE_PER_MINUTE;
                              }
                              else
                                 frequency = OneWireContainer21.ONCE_PER_SECOND;
                           }
                           catch(NumberFormatException nfe)
                           {
                              setStatus(ERRMSG, "Bad Number Format: " + nfe);
                              return;
                           }
                        }
                        synchronized(syncObj)
                        {
                           if(result==JOptionPane.OK_OPTION)
                           {
                              enqueueRunTask(new InitMissionTask(adapter, container,
                                 rollover, syncClock, rate,
                                 delay, low, high, clockalarm,
                                 frequency, second, minute, hour, day));
                              enqueueRunTask(setupViewerTask);
                           }
                        }
                     }
                  }
               }
            );
            JButton disableMissionButton = new JButton("Disable Mission");
            disableMissionButton.addActionListener(new ActionListener()
               {
                  public void actionPerformed(ActionEvent ae)
                  {
                     synchronized(syncObj)
                     {
                        enqueueRunTask(new DisableMissionTask(adapter, container));
                        enqueueRunTask(setupViewerTask);
                     }
                  }
               }
            );
         buttonPanel.add(refreshButton);
         buttonPanel.add(newMissionButton);
         buttonPanel.add(disableMissionButton);
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
                           THERMOCHRON_VIEWER_FAHRENHEIT, true);
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
                           THERMOCHRON_VIEWER_FAHRENHEIT, false);
                        enqueueRunTask(setupViewerTask);
                     }
                  }
               }
            );
         checkboxPanel.add(fahCheck);
         checkboxPanel.add(celCheck);
      commandPanel.add(buttonPanel);
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
      return (owc instanceof OneWireContainer21);
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
            this.container = (OneWireContainer21)owc;
            this.romID = owc.getAddressAsString();
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
      setStatus(VERBOSE, "No Device");
      this.temperaturePlot.resetPlot();
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

   // Calls OneWireContainer21 getOperatingRangeLowTemperature
   // for defalut value when setting Low Alarm.
   public double InitLowTempTask(OneWireContainer21 container)
   {
      return container.getOperatingRangeLowTemperature();
   }

   // Calls OneWireContainer21 getOperatingRangeHighTemperature
   // for default value when setting High Alarm.
   public double InitHighTempTask(OneWireContainer21 container)
   {
      return container.getOperatingRangeHighTemperature();
   }

   // -------------------------------------------------------------------------
   // Viewer Tasks - Tasks for polling temperature devices
   // -------------------------------------------------------------------------
   /**
    * NewMissionTask encapsulates the action of initializing a new mission for
    * a Thermochron Device.
    */
   protected class InitMissionTask extends ViewerTask
   {
      DSPortAdapter task_adapter;
      OneWireContainer21 task_container;
      boolean task_rollover, task_syncClock, task_clockalarm;
      int task_rate, task_delay;
      double task_low, task_high;
      int task_frequency, task_second, task_minute, task_hour, task_day;

      public InitMissionTask(DSPortAdapter adapter, OneWireContainer21 container,
         boolean rollover, boolean syncClock, int rate,
         int delay, double low, double high, boolean clockalarm,
         int frequency, int second, int minute, int hour, int day)
      {
         task_adapter = adapter;
         task_container = container;
         task_rollover = rollover;
         task_syncClock = syncClock;
         task_rate = rate;
         task_delay = delay;
         task_low = low;
         task_high = high;
         task_clockalarm = clockalarm;
         task_frequency = frequency;
         task_second = second;
         task_minute = minute;
         task_hour = hour;
         task_day = day;
      }

      public void executeTask()
      {
         if(task_adapter==null || task_container==null)
            return;
         try
         {
            task_adapter.beginExclusive(true);
            byte[] state = task_container.readDevice();
            boolean  missionActive =
               task_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                                   OneWireContainer21.MISSION_IN_PROGRESS_FLAG,
                                   state);
            // disable current mission
            if(missionActive)
               task_container.disableMission();

            // clear memory contents
            task_container.clearMemory();

            state = task_container.readDevice();

            // rollover enable
            task_container.setFlag(OneWireContainer21.CONTROL_REGISTER,
               OneWireContainer21.ROLLOVER_ENABLE_FLAG, task_rollover, state);

            // temperature alarms
            task_container.setTemperatureAlarm(OneWireContainer21.ALARM_HIGH,
               task_high, state);
            task_container.setTemperatureAlarm(OneWireContainer21.ALARM_LOW,
               task_low, state);

            // clock alarm
            if(task_clockalarm)
            {
               task_container.setClockAlarm(task_hour, task_minute, task_second,
                  task_day, task_frequency, state);
               task_container.setClockAlarmEnable(true, state);
            }

            // clear the alarm flags
            task_container.setFlag(OneWireContainer21.STATUS_REGISTER,
                     OneWireContainer21.TIMER_ALARM_FLAG, false);
            task_container.setFlag(OneWireContainer21.STATUS_REGISTER,
                     OneWireContainer21.TEMPERATURE_HIGH_FLAG, false);
            task_container.setFlag(OneWireContainer21.STATUS_REGISTER,
                     OneWireContainer21.TEMPERATURE_LOW_FLAG, false);

            // synchronize clock to current time
            if(task_syncClock)
            {
               task_container.setClock(new java.util.Date().getTime(), state);
            }

            // mission start delay
            task_container.setMissionStartDelay(task_delay, state);

            // start the clock running
            task_container.setClockRunEnable(true, state);

            // write all registers back to the device
            task_container.writeDevice(state);

            // enable the mission
            task_container.enableMission(task_rate);
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
      OneWireContainer21 task_container = null;

      public DisableMissionTask(DSPortAdapter adapter, OneWireContainer21 container)
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
            byte[] state = task_container.readDevice();
            boolean  missionActive =
               task_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                                   OneWireContainer21.MISSION_IN_PROGRESS_FLAG,
                                   state);
            // disable current mission
            if(missionActive)
            {
               task_container.disableMission();
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
      public void executeTask()
      {
         DSPortAdapter l_adapter = null;
         OneWireContainer21 l_container = null;
         synchronized(syncObj)
         {
            if(adapter==null || container==null)
               return;
            l_adapter = adapter;
            l_container = container;
         }

         setStatus(VERBOSE, "Setting up viewer");
         try
         {
            l_adapter.beginExclusive(true);
            byte[] state = l_container.readDevice();
            boolean  missionActive =
               l_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                                   OneWireContainer21.MISSION_IN_PROGRESS_FLAG,
                                   state);
            lblFeature[IS_ACTIVE].setText(" " + missionActive);
            int sample_rate = l_container.getSampleRate(state);
            int sample_count = l_container.getMissionSamplesCounter(state);
            java.util.Date mission_timestamp
            = l_container.getMissionTimeStamp(state).getTime();
            if(sample_count>0)
            {
               lblFeature[MISSION_START].setText(" " + mission_timestamp);
            }
            else
            {
               lblFeature[MISSION_START].setText(" First sample not yet logged");
            }
            lblFeature[SAMPLE_RATE].setText(" Every " +
                           sample_rate + " minute(s)");
            lblFeature[MISSION_SAMPLES].setText(" " +
                           nf.format(l_container.getMissionSamplesCounter(state)));
            lblFeature[TOTAL_SAMPLES].setText(" " +
                           nf.format(l_container.getDeviceSamplesCounter(state)));
            lblFeature[ROLL_OVER].setText(" " +
                           l_container.getFlag(OneWireContainer21.CONTROL_REGISTER,
                                               OneWireContainer21.ROLLOVER_ENABLE_FLAG,
                                               state));
            if(l_container.getMissionSamplesCounter(state)>2048)
               lblFeature[ROLLED_OVER].setText(" Roll over has occurred");
            else
               lblFeature[ROLLED_OVER].setText(" Roll over has NOT occurred");

            if(!l_container.isClockAlarmEnabled(state))
            {
               lblFeature[NEXT_CLOCK_ALARM].setText(" Disabled");
            }
            else
            {
               lblFeature[NEXT_CLOCK_ALARM].setText(" " +
                  new java.util.Date(l_container.getClockAlarm(state)));
            }

            if(bFahrenheit)
            {
               // read the high temperature alarm setting
               String highAlarmText = nf.format(
                  Convert.toFahrenheit(
                     l_container.getTemperatureAlarm(TemperatureContainer.ALARM_HIGH,
                                                     state)));
               // read the low temperature alarm setting
               String lowAlarmText = nf.format(
                  Convert.toFahrenheit(
                     l_container.getTemperatureAlarm(TemperatureContainer.ALARM_LOW,
                                                     state)));
               lblFeature[HIGH_ALARM].setText(" " + highAlarmText + " °F");
               lblFeature[LOW_ALARM].setText(" " + lowAlarmText + " °F");
            }
            else
            {
               // read the high temperature alarm setting
               String highAlarmText = nf.format(
                  l_container.getTemperatureAlarm(TemperatureContainer.ALARM_HIGH,
                                                  state));
               // read the low temperature alarm setting
               String lowAlarmText = nf.format(
                  l_container.getTemperatureAlarm(TemperatureContainer.ALARM_LOW,
                                                  state));
               // read the high and low temperature alarm settings
               lblFeature[HIGH_ALARM].setText(" " + highAlarmText + " °C");
               lblFeature[LOW_ALARM].setText(" " + lowAlarmText + " °C");
            }

            String alarms = null;
            if(l_container.isClockAlarmEnabled(state)
                     && l_container.isClockAlarming(state))
            {
               alarms = " Clock";
            }
            if(l_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                     OneWireContainer21.TEMPERATURE_HIGH_FLAG, state))
            {
               if(alarms!=null)
                  alarms += ", High Temp";
               else
                  alarms = " High Temp";
            }
            if(l_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                     OneWireContainer21.TEMPERATURE_LOW_FLAG, state))
            {
               if(alarms!=null)
                  alarms += ", Low Temp";
               else
                  alarms = " Low Temp";
            }
            if(alarms!=null)
               lblFeature[ALARMS].setText(alarms);
            else
               lblFeature[ALARMS].setText(" None fired");

            Calendar cal = Calendar.getInstance();
            boolean isSampling = false;
            do
            {
               state = l_container.readDevice();
               cal.setTime(new Date(l_container.getClock(state)));

               isSampling = missionActive && (
                  l_container.getFlag(OneWireContainer21.STATUS_REGISTER,
                           OneWireContainer21.SAMPLE_IN_PROGRESS_FLAG, state) ||
                  (cal.get(Calendar.SECOND)>55) );

               if(isSampling)
               {
                  // wait for current sample to finish
                  try
                  {
                     Thread.sleep(1000);
                  }
                  catch(InterruptedException ie)
                  {;}
               }
            }
            while(isSampling);

            long time = mission_timestamp.getTime()
                        + l_container.getFirstLogOffset(state);
            byte[] log = l_container.getTemperatureLog(state);
            temperaturePlot.resetPlot();
            // plot the temperature log
            for(int i=0; i<log.length; i++)
            {
               double tempPoint = l_container.decodeTemperature(log[i]);
               if(bFahrenheit)
                  tempPoint = Convert.toFahrenheit(tempPoint);
               temperaturePlot.addPoint(tempPoint,
                   df.format(new Date(time)) + (bFahrenheit?",F":",C"));
               time += sample_rate * 60 * 1000;
            }

            // Read the alarm history
            StringBuffer alarmText = new StringBuffer();
            byte[] low_history = l_container.getAlarmHistory(OneWireContainer21.TEMPERATURE_LOW_ALARM);
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

            byte[] high_history = l_container.getAlarmHistory(OneWireContainer21.TEMPERATURE_HIGH_ALARM);
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

            // Temperature Histogram
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
         }
         catch(Exception e)
         {
            setStatus(ERRMSG, "Setup Error: " + e.toString());
         }
         finally
         {
            l_adapter.endExclusive();
         }
         setStatus(VERBOSE, "Done Setting up viewer");
      }
   }

   /**
    * Panel for querying all new mission parameters
    */
   class InitMissionPanel extends JPanel
   {
      final String[] frequencyStrings = {
         "Every Second", "Every Minute",
         "Every Hour", "Every Day", "Every Week"
      };
      JCheckBox chkSyncClock, chkRollover, chkClockAlarm;
      JTextField txtSampleRate,txtStartDelay,txtLowAlarm,txtHighAlarm;
      JRadioButton[] frequencyOptions;
      JTextField txtSecond, txtMinute, txtHour, txtDay, txtWeek;

      public InitMissionPanel()
      {
         super(new BorderLayout(3,3));

         JPanel tempGrid = new JPanel(new GridLayout(3,1));
         JPanel tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
         JPanel tempBox2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            // synchronize clock checkbox
            chkSyncClock = new JCheckBox("Synchronize Real-time Clock? ");
            chkSyncClock.setFont(fontBold);
            chkSyncClock.setHorizontalAlignment(SwingConstants.CENTER);
            // sample rate input
            JLabel lblTemp = new JLabel("Sampling Rate (1 to 255 min.) ");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox1.add(lblTemp);
            txtSampleRate = new JTextField(6);
            txtSampleRate.setFont(fontPlain);
            txtSampleRate.setText("10");
            tempBox1.add(txtSampleRate);
            // start delay input
            lblTemp = new JLabel("Mission Start Delay? ");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox2.add(lblTemp);
            txtStartDelay = new JTextField(6);
            txtStartDelay.setFont(fontPlain);
            txtStartDelay.setText("0");
            tempBox2.add(txtStartDelay);
         tempGrid.add(chkSyncClock);
         tempGrid.add(tempBox1);
         tempGrid.add(tempBox2);

         add(tempGrid, BorderLayout.WEST);

         tempGrid = new JPanel(new GridLayout(3,1));
         tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
         tempBox2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            // enable rollover checkbox
            chkRollover = new JCheckBox("Enable Rollover?");
            chkRollover.setFont(fontBold);
            chkRollover.setHorizontalAlignment(SwingConstants.CENTER);
            // low temperature alarm input
            if(bFahrenheit)
               lblTemp = new JLabel("Temperature Low Alarm? (°F)");
            else
               lblTemp = new JLabel("Temperature Low Alarm? (°C)");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox1.add(lblTemp);
            txtLowAlarm = new JTextField(6);
            txtLowAlarm.setFont(fontPlain);
            txtLowAlarm.setText("-40");
            tempBox1.add(txtLowAlarm);
            // high temperature alarm input
            if(bFahrenheit)
               lblTemp = new JLabel("Temperature High Alarm? (°F)");
            else
               lblTemp = new JLabel("Temperature High Alarm? (°C)");
            lblTemp.setFont(fontBold);
            lblTemp.setForeground(Color.black);
            tempBox2.add(lblTemp);
            txtHighAlarm = new JTextField(6);
            txtHighAlarm.setFont(fontPlain);
            txtHighAlarm.setText("85");
            tempBox2.add(txtHighAlarm);
         tempGrid.add(chkRollover);
         tempGrid.add(tempBox1);
         tempGrid.add(tempBox2);

         add(tempGrid, BorderLayout.EAST);


         JPanel clockAlarm = new JPanel(new BorderLayout());
         clockAlarm.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Clock Alarm Configuration"));
            chkClockAlarm = new JCheckBox("Enable Clock Alarm?");
            chkClockAlarm.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel frequencyPanel = new JPanel(new GridLayout(5,1));
            frequencyPanel.setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(), "Frequency"));

               ButtonGroup frequencyGroup = new ButtonGroup();
               frequencyOptions = new JRadioButton[frequencyStrings.length];
               for(int i=0; i<frequencyOptions.length; i++)
               {
                  frequencyOptions[i] = new JRadioButton(frequencyStrings[i]);
                  frequencyOptions[i].setFont(fontBold);
                  frequencyOptions[i].setHorizontalAlignment(SwingConstants.LEFT);
                  frequencyGroup.add(frequencyOptions[i]);
                  frequencyPanel.add(frequencyOptions[i]);
               }
               frequencyOptions[4].setSelected(true);

            JPanel whenPanel = new JPanel(new GridLayout(4,1));
            whenPanel.setBorder(BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(), "Alarm On"));
               tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
               lblTemp = new JLabel("Day of Week (1 = Sunday)");
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox1.add(lblTemp);
               txtDay = new JTextField(6);
               txtDay.setFont(fontPlain);
               txtDay.setText("1");
               tempBox1.add(txtDay);
            whenPanel.add(tempBox1);
               tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
               lblTemp = new JLabel("Hour of Day (0-23)");
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox1.add(lblTemp);
               txtHour = new JTextField(6);
               txtHour.setFont(fontPlain);
               txtHour.setText("0");
               tempBox1.add(txtHour);
            whenPanel.add(tempBox1);
               tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
               lblTemp = new JLabel("Minute of Hour (0-59)");
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox1.add(lblTemp);
               txtMinute = new JTextField(6);
               txtMinute.setFont(fontPlain);
               txtMinute.setText("0");
               tempBox1.add(txtMinute);
            whenPanel.add(tempBox1);
               tempBox1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
               lblTemp = new JLabel("Second of Minute (0-59)");
               lblTemp.setFont(fontBold);
               lblTemp.setForeground(Color.black);
               tempBox1.add(lblTemp);
               txtSecond = new JTextField(6);
               txtSecond.setFont(fontPlain);
               txtSecond.setText("0");
               tempBox1.add(txtSecond);
            whenPanel.add(tempBox1);

         //tempGrid = new JPanel(new GridLayout(1,2,3,3));
         tempGrid = new JPanel();
         tempGrid.setLayout(new BoxLayout(tempGrid, BoxLayout.X_AXIS));
         tempGrid.add(frequencyPanel);
         tempGrid.add(whenPanel);

         clockAlarm.add(chkClockAlarm, BorderLayout.NORTH);
         clockAlarm.add(tempGrid, BorderLayout.SOUTH);

         add(clockAlarm, BorderLayout.SOUTH);
      }
   }
}
