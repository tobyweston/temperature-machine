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

/**
 * ADElement, element that contains the visual representation of
 * one channel in an A-to-D device.
 *
 * @author SH
 * @version 1.00
 */
public class ADElement extends JPanel
{
   private JLabel[] lblState, lblStateHdr;
   private String[] strHeader = { "AtoD Voltage " };
   private static final int TOTAL_LABELS = 1;
   private static final int AD   = 0;

   private JCheckBox pollElement = null;
   private java.text.NumberFormat nf = null;

   /**
    * Constructs an ADElement with the provided label.
    */
   public ADElement(String channelLabel)
   {
      super(new BorderLayout());
      setBackground(Color.lightGray);

      nf = new java.text.DecimalFormat();
      nf.setMaximumFractionDigits(4);

      JPanel infoPanel = new JPanel(new GridLayout(TOTAL_LABELS+2, 2, 10, 10));
      infoPanel.setBackground(Color.lightGray);

      // title
      JLabel title = new JLabel(channelLabel, JLabel.CENTER);
      title.setOpaque(true);
      title.setFont(Viewer.fontBold);
      title.setForeground(Color.white);
      title.setBackground(Color.darkGray);
      infoPanel.add(title);
      pollElement = new JCheckBox("Include in Poll");
      infoPanel.add(pollElement);
      pollElement.setForeground(Color.black);
      pollElement.setBackground(Color.lightGray);
      pollElement.setFont(Viewer.fontPlain);
      pollElement.setSelected(true);

      lblStateHdr = new JLabel[TOTAL_LABELS];
      lblState = new JLabel[TOTAL_LABELS];
      for (int i = 0; i < TOTAL_LABELS; i++)
      {
         lblStateHdr[i] = new JLabel(strHeader[i], JLabel.RIGHT);
         lblStateHdr[i].setFont(Viewer.fontBold);
         lblStateHdr[i].setOpaque(true);
         lblStateHdr[i].setForeground(Color.black);
         lblStateHdr[i].setBackground(Color.lightGray);
         infoPanel.add(lblStateHdr[i]);

         lblState[i] = new JLabel("", JLabel.LEFT);
         lblState[i].setOpaque(true);
         lblState[i].setFont(Viewer.fontPlain);
         lblState[i].setForeground(Color.black);
         lblState[i].setBackground(Color.white);
         infoPanel.add(lblState[i]);
      }

      add(infoPanel, BorderLayout.CENTER);
   }


   /**
    * Sets the ranges for A-to-D readings.
    * NOT YET SUPPORTED
    */
   public void setADRanges(double[] ranges)
   {
   }

   /**
    * Sets the resolutions for A-to-D readings.
    * NOT YET SUPPORTED
    */
   public void setADResolutions(double[] resolutions)
   {
   }

   /**
    * Sets the value of the A-to-D voltage
    */
   public void setADVoltage(double voltage)
   {
      lblState[AD].setText(" " + nf.format(voltage));
   }

   /**
    * Returns true if this element should be included in A-to-D conversion.
    * @return true if this element should be included in A-to-D conversion.
    */
   public boolean getIncludeAD()
   {
      return pollElement.isSelected();
   }
}

