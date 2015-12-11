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


import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

/**
 * PotentiometerElement, element that contains the visual representation of
 * one wiper in a potentiometer device.
 *
 * @author SH
 * @version 1.00
 */
public class PotentiometerElement extends JPanel
{
   private JLabel[] lblState, lblStateHdr;
   private String[] strHeader = { "Wiper Position ",
                                  "Resistance Value ",
                                  "Charge Pump " };
   private static final int TOTAL_LABELS = 3;
   private static final int POS   = 0;
   private static final int RES   = 1;
   private static final int PUMP  = 2;

   private final java.text.NumberFormat nf = new java.text.DecimalFormat();

   private JScrollBar bar;
   private boolean toggleButtonClick;

   private int maxPos=-1, wiperPos=-1;
   private double maxRes=-1, res=-1;

   /**
    * Creates a PotentiometerElement with the provided label
    */
   public PotentiometerElement(String wiperLabel)
   {
      super(new BorderLayout());
      setBackground(Color.lightGray);

      JPanel infoPanel = new JPanel(new GridLayout(TOTAL_LABELS+3, 2, 10, 10));
      infoPanel.setBackground(Color.lightGray);

      // title
      JLabel title = new JLabel(wiperLabel, JLabel.CENTER);
      title.setOpaque(true);
      title.setFont(Viewer.fontLargeBold);
      title.setForeground(Color.white);
      title.setBackground(Color.darkGray);
      infoPanel.add(title);
      infoPanel.add(new JLabel("", JLabel.LEFT));

      lblStateHdr = new JLabel[TOTAL_LABELS];
      lblState = new JLabel[TOTAL_LABELS];

      for (int i = 0; i < TOTAL_LABELS; i++)
      {
         lblStateHdr[i] = new JLabel(strHeader[i], JLabel.RIGHT);
         lblStateHdr[i].setFont(Viewer.fontBold);
         lblStateHdr[i].setOpaque(true);
         lblStateHdr[i].setForeground(Color.black);
         lblStateHdr[i].setBackground(Color.lightGray);

         lblState[i] = new JLabel("", JLabel.LEFT);
         lblState[i].setFont(Viewer.fontPlain);
         lblState[i].setOpaque(true);
         lblState[i].setForeground(Color.black);
         lblState[i].setBackground(Color.white);

         infoPanel.add(lblStateHdr[i]);
         infoPanel.add(lblState[i]);
      }

      JLabel sliderLabel =  new JLabel("Adjust Wiper ", JLabel.RIGHT);
      sliderLabel.setFont(Viewer.fontBold);
      sliderLabel.setForeground(Color.black);
      sliderLabel.setBackground(Color.lightGray);
      infoPanel.add(sliderLabel);
      bar = new JScrollBar(JScrollBar.HORIZONTAL, 0, 1, 0, 256);
      infoPanel.add(bar);

      bar.addMouseMotionListener(new MouseMotionAdapter()
         {
            public void mouseDragged(MouseEvent e)
            {
               lblState[POS].setText(" " + bar.getValue());
            }
         }
      );

      add(infoPanel, BorderLayout.CENTER);

      toggleButtonClick = false;
      JButton toggleButton = new JButton("Toggle Charge Pump");
      toggleButton.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent event)
            {
               toggleButtonClick = true;
            }
         }
      );
      add(toggleButton, BorderLayout.SOUTH);

   }

   /**
    * Set the value of this wiper's current position.
    *
    * @param pos the current setting of this wiper.
    */
   public void setWiperPosition(int pos)
   {
      if(this.wiperPos!=pos)
      {
         String str = String.valueOf(pos);
         this.wiperPos = pos;
         this.lblState[POS].setText(" "+str);
         this.bar.setValue(pos);
      }
   }


   /**
    * Sets the visual element to indicate whether or not this wiper's
    * charge pump is on.
    *
    * @param pumpState if true, the charge pump for this wiper is indicated
    *        as on.
    */
   public void setChargePump(boolean pumpState)
   {
      lblState[PUMP].setText(" "+(pumpState?"On":"Off"));
   }

   /**
    * Sets the current resistance of this wiper element.  The resistance is
    * a function of the max number of wiper positions, the current wiper
    * position, and the maximum resistance of this potentiometer.
    *
    * @param wiperPos the current wiper position.
    * @param maxPos the maximum number of wiper positions.
    * @param maxRes the maximum resistance of this potentiometer.
    */
   public void setResistance(int wiperPos, int maxPos, double maxRes)
   {
      setWiperPosition(wiperPos);
      setTotalPositions(maxPos);

      double resistance = (maxRes*wiperPos/(maxPos-1));
      if(this.res!=resistance)
      {
         this.res = resistance;
         lblState[RES].setText(" " + nf.format(resistance) + " kOhms");
      }
   }

   /**
    * Set the max number of positions for this wiper.
    *
    * @param max the maximum number of wiper positions.
    */
   public void setTotalPositions(int max)
   {
      if(this.maxPos!=max)
      {
         this.maxPos = max;
         this.bar.setMaximum(max);
      }
   }

   /**
    * Returns the current wiper position as indicated by this graphical
    * element.  If the user updates the graphical element's wiper position,
    * the viewer will update the device to reflect this change.
    *
    * @return the current wiper position.
    */
   public int getSliderPosition()
   {
      return this.bar.getValue();
   }

   /**
    * Returns true if the toggle button has been clicked to toggle the
    * state of the charge pump.
    *
    * @return true if the toggle button has been clicked
    */
   public boolean hasToggleButtonClick()
   {
      return toggleButtonClick;
   }

   /**
    * Clears the state of the toggle button click for toggling the charge pump.
    */
   public void clearButtonClick()
   {
      toggleButtonClick = false;
   }

   /**
    * Returns true if the wiper position of the graphical element has been
    * updated by the user.
    *
    * @return true if the wiper position of the graphical element has been
    * updated by the user.
    */
   public boolean hasSliderMoved()
   {
      return (!this.bar.getValueIsAdjusting() &&
              this.wiperPos!=this.bar.getValue());
   }
}
