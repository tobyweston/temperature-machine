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
 * Frame for the main window of the Tag viewer
 *
 * @version    0.00, 28 Aug 2001
 * @author     DS
 */
public class TagMainFrame extends    JFrame 
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
   protected JPanel northPanel;
   protected JPanel southPanel;
   protected JPanel centerPanel;
   protected JPanel eastPanel;
   protected JPanel westPanel;

   /** Panel contents */
   protected JLabel      logLabel;
   protected JTextField  logField;
   protected JList       pathList;
   protected JCheckBox   scanCheck;
   protected JLabel      portLabel; 
   protected JLabel      statusLabel;
   protected JScrollPane scrollPanel;
   protected DefaultListModel listData;

   /** Log File names */
   protected String logFile;

   //--------
   //-------- Constructors
   //--------

   /** 
    * Constructor a frame to contain the device data.  Provide
    * the device and the log file name
    */
   public TagMainFrame() 
   {
      // construct the frame
      super("1-Wire Tag Viewer");

      //set the look and feel to the system look and feel
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
      mainPanel = new JPanel(new BorderLayout(10,10));

      // create the sub-pannels
      northPanel = new JPanel();
      northPanel.setBorder(BorderFactory.createLoweredBevelBorder()); 

      centerPanel = new JPanel();
      centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

      southPanel = new JPanel();
      southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
      southPanel.setBorder(BorderFactory.createLoweredBevelBorder()); 

      westPanel = new JPanel();
      westPanel.setBorder(BorderFactory.createRaisedBevelBorder()); 
      westPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

      eastPanel = new JPanel();
      eastPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

      // fill the panels
      
      // north
      logLabel = new JLabel("Log Filename: ");
      northPanel.add(logLabel);

      logField = new JTextField("log.txt",20);
      logField.addActionListener(this);
      northPanel.add(logField);

      // center 
      listData = new DefaultListModel(); 
      listData.addElement("                                                                     ");
      listData.addElement("                                                                     ");
      listData.addElement("                                                                     ");
      listData.addElement("                                                                     ");
      pathList = new JList(listData);
      pathList.setVisibleRowCount(5); 
      scrollPanel = new JScrollPane(pathList);
      scrollPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "1-Wire Paths to Search")); 
      centerPanel.add(scrollPanel);

      // west
      scanCheck = new JCheckBox("Scan 1-Wire Paths for XML Tags",false);
      scanCheck.addActionListener(this);
      westPanel.add(scanCheck);

      // south
      portLabel = new JLabel("Adapter:");
      southPanel.add(portLabel);
      
      statusLabel = new JLabel("Status:");
      southPanel.add(statusLabel);

      // add to main
      mainPanel.add(northPanel,BorderLayout.NORTH);
      mainPanel.add(centerPanel,BorderLayout.CENTER);
      mainPanel.add(southPanel,BorderLayout.SOUTH);
      mainPanel.add(eastPanel,BorderLayout.EAST);
      mainPanel.add(westPanel,BorderLayout.WEST);

      // add to frame
      getContentPane().add(mainPanel);

      // pack the frame 
      pack();

      // resize the window and put in random location
      Dimension current_sz = getSize();
      setSize(new Dimension(current_sz.width * 5 / 4,current_sz.height));
      Toolkit tool = Toolkit.getDefaultToolkit();
      Dimension mx = tool.getScreenSize();
      Dimension sz = getSize();
      Random rand = new Random();
      setLocation((mx.width - sz.width) / 2, 
                  (mx.height - sz.height) / 2);

      // clear out the listbox data
      listData.removeAllElements();

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

      // Currently nothing done with event
   }

   /** 
    * Gets the state of the scan check box
    */
   public boolean isScanChecked()
   {
      return scanCheck.isSelected();
   }

   /** 
    * Gets the logfile delay in seconds
    *
    * @return logfile name entered
    */
   public String getLogFile()
   {
      return logField.getText();   
   }

   /** 
    * Sets the status message
    */
   public void setStatus(String newStatus)
   {
      statusLabel.setText("Status: " + newStatus);
      // For easy debug, uncomment this line
      //??System.out.println("Status: " + newStatus); 
   }

   /** 
    * Clear the current path list
    */
   public void clearPathList()
   {
      listData.removeAllElements();
   }

   /** 
    * Add an element to the path list
    */
   public void addToPathList(String newPath)
   {
      listData.addElement(newPath);
   }

   /** 
    * Sets the label for adapter
    */
   public void setAdapterLabel(String newAdapter)
   {
      portLabel.setText("Adapter: " + newAdapter);
   }
}


