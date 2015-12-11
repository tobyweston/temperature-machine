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
import java.awt.*;
import java.awt.event.*;

/**
 * Title: SwitchElement
 * Description: Element that contains the visual representation of
 *              one channel in a switch.
 * Copyright: Copyright (c) 2002
 * Company: Dallas Semiconductor
 * @author DS
 * @version 1.10
 */
public class SwitchElement
   extends    JPanel
   implements ActionListener
{

   //--------
   //-------- Finals
   //--------

   /** Field STATE - index into state label array for 'STATE' */
   private static final int STATE = 0;

   /** Field LEVEL - index into state label array for level */
   private static final int LEVEL = 1;

   /** Field ACTV - index into state label array for level */
   private static final int ACTV = 2;

   //--------
   //-------- Variables
   //--------

   /** Field title - label for the title of the element */
   private JLabel title = null;

   /** Field dummy           */
   private JLabel dummy = null;

   /** Field stateLabel           */
   private JLabel[] stateLabel = null;

   /** Field state           */
   private JLabel[] state = null;

   /** Field toggleButton           */
   private JButton toggleButton = null;

   /** Field clearButton           */
   private JButton clearButton = null;

   /** Field toggleButtonClick           */
   private boolean toggleButtonClick;

   /** Field clearButtonClick           */
   private boolean clearButtonClick;

   //--------
   //-------- Constructors
   //--------

   /**
    * Construct a SwitchElement with the provided title
    * and features.
    *
    * @param channelTitle - String representing the title of the element
    * @param showState - 'true' to display state and toggle button
    * @param showActivity - 'true' to display activity state and button clear
    * @param showLevel - 'true' to display level state
    */
   public SwitchElement(String channelTitle, boolean showState, boolean showActivity, boolean showLevel)
   {
      super(new GridLayout(5, 2, 10, 10));

      setBackground(Color.lightGray);

      // title
      title = new JLabel(channelTitle, JLabel.CENTER);
      title.setOpaque(true);
      title.setFont(Viewer.fontLargeBold);
      title.setForeground(Color.white);
      title.setBackground(Color.darkGray);
      add(title);
      dummy = new JLabel("", JLabel.LEFT);
      add(dummy);

      // state grid
      stateLabel         = new JLabel [3];
      stateLabel [STATE] = new JLabel("State", JLabel.RIGHT);
      stateLabel [LEVEL] = new JLabel("Level", JLabel.RIGHT);
      stateLabel [ACTV]  = new JLabel("Activity", JLabel.RIGHT);
      state              = new JLabel [3];
      state [STATE]      = new JLabel("", JLabel.CENTER);
      state [LEVEL]      = new JLabel("", JLabel.CENTER);
      state [ACTV]       = new JLabel("", JLabel.CENTER);
      for (int i = 0; i < 3; i++)
      {
         stateLabel [i].setOpaque(true);
         stateLabel [i].setFont(Viewer.fontBold);
         stateLabel [i].setForeground(Color.black);
         stateLabel [i].setBackground(Color.lightGray);
         add(stateLabel [i]);
         state [i].setOpaque(true);
         state [i].setFont(Viewer.fontPlain);
         state [i].setForeground(Color.black);
         state [i].setBackground(Color.white);
         add(state [i]);
      }

      // buttons
      toggleButton = new JButton("Toggle State");
      add(toggleButton);

      clearButton = new JButton("Clear Activity");
      add(clearButton);

      // button listeners
      toggleButton.addActionListener(this);
      clearButton.addActionListener(this);

      // init state
      toggleButtonClick = false;
      clearButtonClick  = false;

      // disable features not used
      if (!showState)
      {
         stateLabel [STATE].setVisible(false);
         state [STATE].setVisible(false);
         toggleButton.setVisible(false);
      }

      if (!showActivity)
      {
         stateLabel [ACTV].setVisible(false);
         state [ACTV].setVisible(false);
         clearButton.setVisible(false);
      }

      if (!showLevel)
      {
         stateLabel [LEVEL].setVisible(false);
         state [LEVEL].setVisible(false);
      }
   }

   //--------
   //-------- Methods
   //--------

   /**
    * Sets the state value string
    *
    * @param stateStr string representing the switch state
    */
   public void setState(String stateStr)
   {
      if (stateStr != state [STATE].getText())
         state [STATE].setText(stateStr);
   }

   /**
    * Sets the Level value string
    *
    * @param levelStr string representing the level state
    *
    */
   public void setLevel(String levelStr)
   {
      if (levelStr != state [LEVEL].getText())
         state [LEVEL].setText(levelStr);
   }

   /**
    * Sets the activity value string
    *
    * @param activityStr string representing the activity value
    */
   public void setActivity(String activityStr)
   {
      if (activityStr != state [ACTV].getText())
         state [ACTV].setText(activityStr);
   }

   /**
    * Action listener for the buttion clicks
    *
    * @param event button event
    */
   public void actionPerformed(ActionEvent event)
   {
      Object source = event.getSource();

      if (source == toggleButton)
         toggleButtonClick = true;
      else if (source == clearButton)
         clearButtonClick = true;
   }

   /**
    * Checks to see if the toggle button has been clicked
    *
    * @return 'true' if the toggle button has been clicked
    */
   public boolean hasToggleButtonClick()
   {
      boolean click = toggleButtonClick;

      return click;
   }

   /**
    * Chechs to see if the clear activity button has been clicked
    *
    * @return 'true' if the clear activity button has been clicked
    */
   public boolean hasClearButtonClick()
   {
      boolean click = clearButtonClick;

      return click;
   }

   /**
    * Clears any pending button click notifications
    */
   public void buttonClickClear()
   {
      toggleButtonClick = false;
      clearButtonClick  = false;
   }
}
