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

import com.dalsemi.onewire.application.tag.TaggedDevice;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.text.NumberFormat;
import java.io.*;

/**
 * Base window/frame class for the tagged device sensor/actuators.
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class DeviceFrame extends    JFrame 
                         implements ActionListener
{
   //--------
   //-------- Variables
   //--------

   /** Tagged device that this frame displays */
   protected TaggedDevice dev;

   /** Main Panel */
   protected JPanel mainPanel;

   /** Sub panels */
   protected JPanel topPanel;
   protected JPanel centerPanel;
   protected JPanel bottomPanel;

   /** Panel contents */
   protected JLabel    mainLabel; 
   protected JLabel    timeLabel; 
   protected JLabel    clusterLabel;
   protected JLabel    pathLabel;
   protected JCheckBox logCheck;
   protected JButton   readButton;
   protected JComboBox pollCombo;

   /** delay time in seconds */
   protected int pollDelay;
   
   /** Button click state */
   protected boolean readButtonClick;

   /** Utility to format numbers */
   protected NumberFormat num_format;

   /** Last reading for tracking of a 'change' */
   protected String lastReading;

   /** LogFile name */
   protected String logFile;

   //--------
   //-------- Constructors
   //--------

   /** 
    * Constructor a frame to contain the device data.  Provide
    * the device and the log file name
    */
   public DeviceFrame(TaggedDevice dev, String logFile) 
   {
      // construct the frame
      super(dev.DeviceContainer.getAddressAsString());

      // init
      pollDelay = 0;
      readButtonClick = false; 
      num_format = NumberFormat.getInstance();
      num_format.setMaximumFractionDigits(2);
      num_format.setMinimumFractionDigits(0);
      num_format.setMinimumIntegerDigits(2);
      num_format.setGroupingUsed(false);
      lastReading = new String("none");

      // get ref to the tagged device and log file
      this.dev = dev;
      this.logFile = logFile;

      // set the look and feel to the system look and feel
      try
      {
         UIManager.setLookAndFeel(
             UIManager.getSystemLookAndFeelClassName());
      } 
      catch (Exception e) 
      {
         e.printStackTrace();
      }

      // add an event listener to end the aplication when the frame is closed
      addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {System.exit(0);}
      });

      // create the main panel
      mainPanel = new JPanel(new GridLayout(3,1));

      // create the sub-panels
      topPanel = new JPanel();
      topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
      topPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

      centerPanel = new JPanel();
      centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
      centerPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
      centerPanel.setBackground(Color.white);

      bottomPanel = new JPanel();
      bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
      bottomPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

      // fill the panels
      // top
      clusterLabel = new JLabel("Cluster: " + dev.getClusterName());
      topPanel.add(clusterLabel);

      mainLabel = new JLabel(dev.getLabel());
      mainLabel.setHorizontalAlignment(JLabel.CENTER); 
      mainLabel.setFont(new Font("SansSerif",Font.PLAIN,20));
      topPanel.add(mainLabel);

      logCheck = new JCheckBox("Logging Enable",false);
      logCheck.addActionListener(this);
      topPanel.add(logCheck);

      // center
      timeLabel = new JLabel("Last Reading: none");
      timeLabel.setHorizontalAlignment(JLabel.CENTER); 
      centerPanel.add(timeLabel);

      // bottom
      readButton = new JButton("Read Once");
      readButton.setAlignmentX(Component.LEFT_ALIGNMENT);
      readButton.addActionListener(this);
      bottomPanel.add(readButton);      
 
      String[] selectionStrings = { "No Polling", "1 second", "30 seconds", "1 minute", "10 minutes", "1 hour" };
      pollCombo = new JComboBox(selectionStrings);
      pollCombo.setEditable(false);
      pollCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
      pollCombo.addActionListener(this);
      bottomPanel.add(pollCombo);      

      pathLabel = new JLabel("Path: " + dev.getOWPath().toString());
      pathLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
      bottomPanel.add(pathLabel);

      // add to main
      mainPanel.add(topPanel);
      mainPanel.add(centerPanel);
      mainPanel.add(bottomPanel);

      // add to frame
      getContentPane().add(mainPanel);

      // pack the frame 
      pack();

      // resize the window and put in random location
      Dimension current_sz = getSize();
      setSize(new Dimension(current_sz.width * 3 / 2,current_sz.height));
      Toolkit tool = Toolkit.getDefaultToolkit();
      Dimension mx = tool.getScreenSize();
      Dimension sz = getSize();
      Random rand = new Random();
      setLocation(rand.nextInt((mx.width - sz.width) / 2), 
                  rand.nextInt((mx.height - sz.height) / 2));

      // make visible
      setVisible(true);
   }

   //--------
   //-------- Methods
   //--------

   /** 
    * Implements the ActionListener interface to handle  
    * button click and data change events
    */
   public void actionPerformed(ActionEvent event)
   {
      Object source = event.getSource();

      if (source == readButton)
      {
         readButtonClick = true;
      }
      else if (source == pollCombo)
      {
         switch (pollCombo.getSelectedIndex())
         {
            case 0: pollDelay = 0; break;
            case 1: pollDelay = 1; break;
            case 2: pollDelay = 30; break;
            case 3: pollDelay = 60; break;
            case 4: pollDelay = 600; break;
            case 5: pollDelay = 3600; break;
         }
      }
   }

   /** 
    * Gets the state of the log check box
    */
   public boolean isLogChecked()
   {
      return logCheck.isSelected();
   }

   /** 
    * Checks to see if the read button has been clicked.  The
    * state gets reset with this call.
    */
   public boolean getReadButtonClick()
   {
      boolean rt = readButtonClick;
      readButtonClick = false;
      return rt;
   }

   /** 
    * Gets the poll delay in seconds
    */
   public int getPollDelay()
   {
      return pollDelay;   
   }

   /** 
    * Sets reading time label to the current time
    */
   public void showTime(String header)
   {
      // print time stamp
      GregorianCalendar calendar = new GregorianCalendar();
      lastReading = num_format.format(calendar.get(Calendar.MONTH) + 1) + "/" +
                       num_format.format(calendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                       num_format.format(calendar.get(Calendar.YEAR)) + " " +
                       num_format.format(calendar.get(Calendar.HOUR)) + ":" +
                       num_format.format(calendar.get(Calendar.MINUTE)) + ":" +
                       num_format.format(calendar.get(Calendar.SECOND)) + "." +
                       num_format.format(calendar.get(Calendar.MILLISECOND)/10) + 
                       ((calendar.get(Calendar.AM_PM) == 0) 
                       ? "AM" : "PM");
      timeLabel.setText(header + lastReading);
   }

   /** 
    * Hides the 'read' items when frame is in actuator mode
    */
   public void hideReadItems()
   {
      pollCombo.setVisible(false); 
      readButton.setVisible(false);
   }

   /** 
    * Logs the current reading with the provided value
    */
   public void log(String value)
   {
      // construct the string to log
      String log_string = new String(dev.getClusterName() + "," +
                                     mainLabel.getText() + "," +
                                     dev.getOWPath().toString() + 
                                     getTitle() + "," + 
                                     lastReading + "," +  
                                     value);
      try
      {
         PrintWriter writer = new PrintWriter(new FileOutputStream(logFile, true));
         writer.println(log_string);
         writer.flush();
         writer.close();
      }
      catch (FileNotFoundException e)
      {
         System.out.println(e);
      }
   }

}


