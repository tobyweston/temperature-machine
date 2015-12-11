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
import java.awt.image.*;

/**
 * A graphical, general-purpose meter.  For displaying values such as
 * temperature or % relative humidity readings.
 *
 * @author SH
 * @version 1.0
 */
class AdjustableMeter extends JPanel
{
   private double min, max, range, currentValue, lastValue;
   private int labelRate = 10;
   private BufferedImage img = null;
   private Dimension currentSize = null;
   private Rectangle updateRange = null;
   private Rectangle currentRange = null;
   private java.text.NumberFormat nf = null;

   /**
    * Constructs a new Adjustable Meter with the range (0,100)
    */
   public AdjustableMeter()
   {
      this(0,100);
   }

   /**
    * Constructs a new Adjustable Meter with the provided range.
    *
    * @param minValue the minimum of the range
    * @param maxValue the maximum of the range
    */
   public AdjustableMeter(double minValue, double maxValue)
   {
      setScale(minValue, maxValue);

      setPreferredSize(new Dimension(150, 250));

      nf = new java.text.DecimalFormat();
      nf.setMaximumFractionDigits(1);
   }

   /**
    * Sets the frequency at which labels appear on the side of the meter.
    * The units can be thought of as "amount of space per label", so a
    * larger label rate means more spacing between each label.
    *
    * @param rate the rate at which labels should be placed.
    */
   public void setLabelRate(int rate)
   {
      this.labelRate = rate;
      this.currentSize = null;
      repaint();
   }

   /**
    * Sets the scale of the meter.
    *
    * @param minValue the minimum of the range
    * @param maxValue the maximum of the range
    */
   public void setScale(double minValue, double maxValue)
   {
      this.min = minValue;
      this.max = maxValue;
      this.range = Math.abs(maxValue-minValue);
      this.currentValue = minValue;
      this.currentSize = null;
      repaint();
   }

   /**
    * Set the current value of the meter.
    *
    * @param val the current value of the meter.
    */
   public void setValue(double val)
   {
      this.currentValue = val;
      repaint();
   }

   /**
    * Reset the value back to the minimum.
    */
   public void resetValue()
   {
      this.currentValue = this.min;
      repaint();
   }

   /**
    * Paints this meter.
    */
   public void paint(Graphics g)
   {
      update(g);
   }

   /**
    * Paints this meter.
    */
   public void update(Graphics g)
   {
      Graphics2D g2 = (Graphics2D)g;
      Dimension d = getSize();
      int w = d.width;
      int h = d.height;

      if(currentSize==null)
      {
         //create buffered offscreen image
         this.currentSize = d;
         this.img = (BufferedImage)createImage(w, h);
         Graphics2D big = img.createGraphics();
         Rectangle rect = new Rectangle(60, h-50);
         rect.setLocation(w/2-30, 25);
         big.setStroke(new BasicStroke(2.0f));
         big.setColor(Color.blue);
         big.draw(rect);
         this.lastValue = this.min-1;
         updateRange = new Rectangle(40, h-100);
         updateRange.setLocation(w/2-20, 50);

         //labels
         boolean leftSide = true;
         int leftStartX = (int)updateRange.getX();
         int rightStartX = (int)(leftStartX + updateRange.getWidth());
         int startY = (int)updateRange.getY();
         int lastY = (int)(startY + updateRange.getHeight());
         int numLabels = (int)Math.round(updateRange.getHeight()/labelRate);
         double diff = (range/numLabels);
         double val = this.max;

         big.drawLine(rightStartX, startY,
                      rightStartX+10, startY);
         big.drawString(nf.format(val), rightStartX+12, startY);
         for(int i=1; i<numLabels; i++, leftSide=!leftSide)
         {
            val -= diff;
            int Y = startY+(labelRate*i);
            if(leftSide)
            {
               big.drawLine(leftStartX, Y,
                            leftStartX-10, Y);
               big.drawString(nf.format(val), leftStartX-50, Y);
            }
            else
            {
               big.drawLine(rightStartX, Y,
                            rightStartX+10, Y);
               big.drawString(nf.format(val), rightStartX+12, Y);
            }
         }
         if(leftSide)
         {
            big.drawLine(leftStartX, lastY,
                         leftStartX-10, lastY);
            big.drawString(nf.format(this.min), leftStartX-50, lastY);
         }
         else
         {
            big.drawLine(rightStartX, lastY,
                         rightStartX+10, lastY);
            big.drawString(nf.format(this.min), rightStartX+12, lastY);
         }
      }

      if(lastValue!=currentValue)
      {
         Graphics2D big = (Graphics2D)img.getGraphics();
         big.setColor(Color.gray);
         big.fill(updateRange);

         double percent = (this.currentValue-this.min)/range;
         double height = updateRange.getHeight();
         double diff = height - Math.abs(height*percent);
         currentRange = new Rectangle((int)updateRange.getWidth(),
                                      (int)Math.round(height-diff));
         currentRange.setLocation((int)updateRange.getX(),
                                  (int)Math.round(updateRange.getY()+diff));

         big.setColor(Color.red);
         big.fill(currentRange);

         this.lastValue = this.currentValue;
      }

      g2.drawImage(img, 0, 0, this );

      if(!d.equals(currentSize))
         currentSize = null;
   }
}