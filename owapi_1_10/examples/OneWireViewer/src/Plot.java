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

import javax.swing.JPanel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.datatransfer.*;

/**
 * A 2D plot used for plotting temperatures.  The Y-Axis is labeled, but
 * the X-Axis is not.  The labels for the Y-Axis can be set with
 * <code>setScale(double,double)</code>.
 *
 * @author SH
 * @version 1.00
 */
public class Plot extends JPanel
   implements Cloneable, ActionListener, MouseListener
{
   /** the rate at which grid lines are laid on the Y-Axis */
   private static final int Y_GRID_RATE = 20;
   /** the rate at which grid lines are laid on the X-Axis */
   private static final int X_GRID_RATE = 30;
   /** the starting offset for grid lines on the X-Axis */
   private static final int X_OFFSET = 40;
   /** the ending buffer space for labels on the Y-Axis */
   private static final int Y_BUFFER = 20;
   /** the size of a point, a single entry, on the plot */
   private static final int POINT_SIZE = 4;
   /** the initial size of the array for holding points */
   private static final int INITIAL_AMOUNT = 32;
   /** default maximum size of the array for holding points */
   private static final int MAXIMUM_AMOUNT = 8192;
   /** the rate at which to grow the array for holding points */
   private static final double GROWTH_RATE = .3;

   /** array for holding all points added to the plot */
   private double[] points;
   /** the maximum size of the array for holding points */
   private int maxPoints;
   /** the next empty slot in point array, and the last index that was drawn */
   private int pointIndex = 0, lastIndexDrawn = -1;
   /** the min, max, and range (abs[max-min]) of the points */
   private double min = 0, max = 100, range = 100;
   /** if autoScaling is true, the scale will be set so all points are visible */
   private boolean autoScale = true;

   /** array for holding string labels for each point */
   private String[] pointsLabel;

   /** off-screen buffered image, for faster updates */
   private BufferedImage img = null;
   /** The current size of the buffered image */
   private Dimension currentSize = null;
   /** Strokes used for drawing the grid on the plot */
   private BasicStroke dashed = null, normal = null;
   /** for formating numeric strings */
   private static final java.text.NumberFormat nf = 
      new java.text.DecimalFormat();

   /** context-menu for pop-up, allows copying data to the scratchpad */
   private JPopupMenu data = null;

   /**
    * Creates a default plot.  Uses the default initial and maximum amount of points.
    */
   public Plot()
   {
      this(INITIAL_AMOUNT, MAXIMUM_AMOUNT);
   }

   /**
    * Creates a plot with the provided initial and maximum amount of points.
    *
    * @param initialAmount the initial amount of points this plot should display.
    * @param maximumAmount the maximum amount of points this plot should display.
    */
   public Plot(int initialAmount, int maximumAmount)
   {
      // initialize the storage
      points = new double[initialAmount];
      pointsLabel = new String[initialAmount];
      // save the user-specified maximum amount
      maxPoints = maximumAmount;

      // create the number format object
      nf.setMaximumFractionDigits(1);
      nf.setGroupingUsed(false);

      // create the normal and dashed strokes for drawing tick marks
      normal = new BasicStroke(1);
      dashed = new BasicStroke(1,
                               BasicStroke.CAP_BUTT,
                               BasicStroke.JOIN_BEVEL,
                               0,   // Miter limit
                               new float[] {4,4}, // 4-on, 4-off
                               0);  // Dash phase

      // popup-menu for getting data to the clipboard
      data = new JPopupMenu("Data");
      // copy data with comm-separated values
      JMenuItem copyDataComma = new JMenuItem("Copy Data to Clipboard (comma-separated)");
      copyDataComma.setActionCommand("CopyComma");
      copyDataComma.addActionListener(this);
      // copy data with newline-separated values
      JMenuItem copyDataLabel = new JMenuItem("Copy Data to Clipboard with Labels");
      copyDataLabel.setActionCommand("CopyWithLabels");
      copyDataLabel.addActionListener(this);
      // copy data with newline-separated values
      JMenuItem copyData = new JMenuItem("Copy Data to Clipboard without Labels");
      copyData.setActionCommand("CopyWithoutLabels");
      copyData.addActionListener(this);
      JMenuItem rescale = new JMenuItem("Rescale Graph");
      rescale.setActionCommand("RescaleGraph");
      rescale.addActionListener(this);
      data.add(copyDataComma);
      data.add(copyDataLabel);
      data.add(copyData);
      data.add(rescale);
      this.add(data);

      // add mouse listener to this panel so we can grab the right-click event
      this.addMouseListener(this);
   }

   /**
    * Handle's pop-up menu, which copies data points to the clipboard.
    *
    * @param ae the popup menu action event
    */
   public void actionPerformed(ActionEvent ae)
   {
      int currIndex = pointIndex;
      double[] currPoints = points;
      String[] currPointsLabel = pointsLabel;
      int oldMaxFractionDigits = nf.getMaximumFractionDigits();
      nf.setMaximumFractionDigits(3);
      if(currIndex>0)
      {
         if(ae.getActionCommand().equals("RescaleGraph"))
         {
            this.recalculateScale();
         }
         else
         {
            StringBuffer sb = new StringBuffer();
            if(ae.getActionCommand().equals("CopyWithoutLabels"))
            {
               for(int i=0; i<currIndex; i++)
               {
                  sb.append(nf.format(currPoints[i]));
                  sb.append("\n");
               }
            }
            else if(ae.getActionCommand().equals("CopyWithLabels"))
            {
               for(int i=0; i<currIndex; i++)
               {
                  sb.append(currPointsLabel[i]);
                  sb.append(',');
                  sb.append(nf.format(currPoints[i]));
                  sb.append("\n");
               }
            }
            else if(ae.getActionCommand().equals("CopyComma"))
            {
               sb.append(nf.format(currPoints[0]));
               for(int i=1; i<currIndex; i++)
               {
                  sb.append(",");
                  sb.append(nf.format(currPoints[i]));
               }
            }
            StringSelection ss = new StringSelection(sb.toString());
            Toolkit tk = Toolkit.getDefaultToolkit();
            tk.getSystemClipboard().setContents(ss, ss);
         }
      }
      nf.setMaximumFractionDigits(oldMaxFractionDigits);
   }

   /**
    * Returns the maximum number of points this plot can display.
    *
    * @return the maximum number of points this plot can display.
    */
   public int getMaxPoints()
   {
      return this.maxPoints;
   }

   /**
    * Sets the maximum number of points this plot can display.
    *
    * @param max the maximum number of points this plot can display.
    */
   public void setMaxPoints(int max)
   {
      this.maxPoints = max;
   }

   /**
    * Returns whether or not auto-scaling is enabled.  With auto-scaling
    * enabled, points added outside of the scale of the plot will cause the
    * scale to adjust, to include those points.
    *
    * @return <code>true</code> if auto-scaling is enabled
    */
   public boolean getAutoScale()
   {
      return this.autoScale;
   }

   /**
    * Sets whether or not auto-scaling is enabled.  With auto-scaling
    * enabled, points added outside of the scale of the plot will cause the
    * scale to adjust, to include those points.
    *
    * @param scale if <code>true</code>, auto-scaling is enabled
    */
   public void setAutoScale(boolean scale)
   {
      this.autoScale = scale;
   }
   
   public void recalculateScale()
   {
      int currIndex = pointIndex;
      double[] currPoints = points;
      double minValue, maxValue;
      minValue = currPoints[0];
      maxValue = currPoints[0];
      for(int i=0; i<currIndex; i++)
      {
         double pt = currPoints[i];
         if(pt<minValue)
            minValue = pt;
         else if(pt>maxValue)
            maxValue = pt;
      }
      
      double rangeValue = Math.abs(maxValue-minValue);
      //System.out.println("maxValue=" + maxValue + ", minValue=" + minValue + ", rangeValue=" + rangeValue);
      if(rangeValue==0.0)
      {
         rangeValue = Math.abs(Math.log(maxValue));
      }

      minValue -= (rangeValue*0.1 + .2);
      maxValue += rangeValue*0.1;
      setScale(minValue, maxValue);
   }

   /**
    * Set the scale for this plot.
    *
    * @param minValue the minimum value
    * @param maxValue the maximum value
    */
   public void setScale(double minValue, double maxValue)
   {
      this.min = minValue;
      this.max = maxValue;
      this.range = Math.abs(maxValue-minValue);
      this.img = null;
      repaint();
   }

   int displayOffset;
   int displayLength;
   public void setDisplayRange(int displayOffset, int displayLength)
   {
      this.displayOffset = displayOffset;
      this.displayLength = Math.min(displayLength+displayOffset, this.pointIndex);
      this.img = null;
      repaint();
   }

   public void setDisplayAllPoints()
   {
      this.displayOffset = 0;
      this.displayLength = this.points.length;
      this.img = null;
      repaint();
   }


   /**
    * Returns an array with all points that have been added to the plot.
    *
    * @return an array with all points that have been added to the plot.
    */
   public double[] getPoints()
   {
      double[] temp = new double[this.pointIndex];
      System.arraycopy(this.points, 0, temp, 0, temp.length);
      return temp;
   }

   /**
    * Sets the plot to draw all points in the given array.
    *
    * @param newPoints array with all points that will be displayed.
    */
   public void setPoints(double[] newPoints)
   {
      System.arraycopy(newPoints, 0, this.points, 0, newPoints.length);
      this.pointIndex = newPoints.length;
      this.lastIndexDrawn = -1;
      this.displayLength = this.pointIndex;
      this.displayOffset = 0;
      this.img = null;
      repaint();
   }

   /**
    * Adds a point to the plot.
    *
    * @param value the point to add to the plot.
    */
   public void addPoint(double value)
   {
      addPoint(value, null);
   }

   /**
    * Adds a point to the plot.
    *
    * @param value the point to add to the plot.
    */
   public void addPoint(double value, String label)
   {
      int maxIndex = this.points.length;

      // make sure the current slot isn't beyond the end of the array
      if(this.pointIndex == maxIndex)
      {
         // make sure we haven't already reached the maximum number of points
         if(maxIndex==maxPoints)
         {
            // we've maxed out, subtract one from the beginning so we can
            // add this new point.
            System.arraycopy(this.pointsLabel, 1, this.pointsLabel, 0, maxPoints-1);
            System.arraycopy(this.points, 1, this.points, 0, maxPoints-1);
            this.pointIndex--;
         }
         else
         {
            // we haven't maxed out yet.  let's 'grow' the array
            int newLength = (int)Math.round(maxIndex*(1+GROWTH_RATE));
            int start = 0;

            // don't grow it larger than the maximum number of points
            if(newLength>maxPoints)
            {
               newLength = maxPoints;
               start = 0;//(int)Math.round(maxPoints*GROWTH_RATE/2);
            }

            if(displayLength==this.pointIndex)
              displayLength = newLength;

            // populate the new label array
            String[] tempstr = new String[newLength];
            System.arraycopy(this.pointsLabel, start, tempstr, 0, (maxIndex-start));
            this.pointsLabel = tempstr;

            // populate the new point array
            double[] temp = new double[newLength];
            System.arraycopy(this.points, start, temp, 0, (maxIndex-start));
            this.points = temp;
            if(displayLength==this.pointIndex)
              displayLength = maxIndex-displayOffset;
            this.pointIndex = maxIndex-start;
         }
         this.img = null;
      }


      // add the point
      this.pointsLabel[this.pointIndex] = label;
      this.points[this.pointIndex++] = value;

      // if autoscaling is set, make sure this point is visible
      if(this.autoScale)
      {
         if(this.range==0.0)
         {
            this.range = 10;
         }
         double delta = this.range*0.1;
         if(delta < 0.2)
            delta = 0.2;
         //check for rescale
         if(value-delta<this.min)
         {
            this.min = value - delta;
            this.max = this.points[0];
            for(int i=0; i<this.pointIndex; i++)
               if(this.max<this.points[i])
                  this.max = this.points[i];
            this.max += delta;
            this.range = Math.abs(this.max-this.min);
            this.img = null;
         }
         else if(value+delta>this.max)
         {
            this.max = value + delta;
            this.min = this.points[0];
            for(int i=0; i<this.pointIndex; i++)
               if(this.min>this.points[i])
                  this.min = this.points[i];
            this.min -= delta;
            this.range = Math.abs(this.max-this.min);
            this.img = null;
         }
         if(this.range==0.0)
         {
            this.range = 10;
         }
      }
      
      // draw the plot
      repaint();
   }

   /**
    * Clears all points from the plot.
    */
   public void resetPlot()
   {
      this.pointIndex = 0;
      this.currentSize = null;
      this.img = null;
      this.points = new double[INITIAL_AMOUNT];
      this.pointsLabel = new String[INITIAL_AMOUNT];
      this.displayLength = this.points.length;
      this.displayOffset = 0;
      repaint();
   }

   /**
    * overrriden to just call update
    *
    * @param g the Graphics object
    */
   public void paint(Graphics g)
   {
      update(g);
   }

   /**
    * overriden to draw the plot rather than the JPanel's default draw.
    *
    * @param g the Graphics object
    */
  /*public void update(Graphics g)
   {
      BufferedImage imgTemp = this.img;
      // need Graphics2D to set the stroke
      Graphics2D g2 = (Graphics2D)g;
      // get the current size of the panel
      Dimension d = getSize();
      int w = d.width;
      int h = d.height;
      // the amount to increment along the X-Axis between tick marks
      double xInc = ((double)w-X_START_OFFSET)/points.length;

      // check to see if we need to recreate our off-screen image
      if(imgTemp==null || this.currentSize==null || !d.equals(currentSize))
      {
         // create the buffered image
         imgTemp = (BufferedImage)createImage(w, h);
         // save the size of the image
         this.currentSize = d;
         // get the graphics object and initialize
         Graphics2D big = imgTemp.createGraphics();
         big.setStroke(normal);
         // paint the background white
         big.setColor(Color.white);
         big.fillRect(0,0,w,h);

         // draw the X-Axis tick marks
         big.setStroke(dashed); // Set this line stroking style

         // draw the Y-Axis tick marks
         boolean dark = false;
         double numLines = (h/Y_GRID_RATE);
         for(int i=1; i<numLines+1; i++)
         {
            if(dark=!dark) // major tick (intentional assignment)
              big.setColor(Color.darkGray);
            else // minor tick
              big.setColor(Color.lightGray);
            big.drawLine(0, i*Y_GRID_RATE, w, i*Y_GRID_RATE);
            String lbl = nf.format(this.max - (i/(numLines))*this.range);
            big.setColor(Color.blue);
            big.drawString(lbl,0,i*Y_GRID_RATE);
         }

         // vary the amount of points between tick marks
         //int iInc = 2*((int)(points.length/32));
         int iInc = 2*((int)(Math.max(this.displayLength,32)/32));
         int x = X_OFFSET;
         boolean drawLabels = false;//(pointsLabel[0]!=null);
         int bottomMark = (drawLabels)?h-20:h;
         boolean upDown = true;
         int labelIndex = 0, labelInc = (pointIndex/9);
         for(int i=iInc; x<w; i+=iInc)
         {
            if((i-iInc)%8==0) // major tick
            {
               big.setColor(Color.darkGray);
               big.drawLine(x, 0, x, bottomMark);
               //if(drawLabels && pointsLabel[labelIndex]!=null)
               //{
               //   big.setColor(Color.blue);
               //   if( upDown=!upDown )
               //      big.drawString(pointsLabel[labelIndex], x-10, h);
               //   else
               //      big.drawString(pointsLabel[labelIndex], x-10, h-10);
               //   labelIndex += labelInc;
               //}
            }
            else // minor tick
            {
               big.setColor(Color.lightGray);
               big.drawLine(x, 0, x, bottomMark);
            }
            x = (int)Math.round((xInc*i) + X_OFFSET);
         }

         // reset the last index drawn, since none have been drawn yet
         this.lastIndexDrawn = -1;
         this.img = imgTemp;
      }

      // check to see if there are any new points to draw
      if(pointIndex>0 && pointIndex!=lastIndexDrawn)
      {
         // calculate the starting point
         //int i = (lastIndexDrawn<0)?0:lastIndexDrawn-1;
         int i = (lastIndexDrawn<0)?this.displayOffset:lastIndexDrawn-1;
         double minValue = this.min;
         // figure out the Y value for the starting point
         double percentY = (points[this.displayOffset+i] - minValue)/range;
         int oldY = (int)Math.round(h - Math.abs(h*percentY));
         // figure out  the X value for the starting point
         int oldX = (int)Math.round(xInc*i + X_OFFSET);
         // get the Graphics2D object
         Graphics2D big = (Graphics2D)imgTemp.getGraphics();
         big.setColor(Color.blue);
         big.setStroke(normal);

         // if this is the first point drawn, it needs to be filled
         if(lastIndexDrawn<0)
            big.fillOval(oldX-(POINT_SIZE/2), oldY-(POINT_SIZE/2),
                          POINT_SIZE, POINT_SIZE);

         // go ahead and advance i to the next point
         i++;
         // now draw the rest of the points
         for(; i<pointIndex; i++)
         {
            // figure out the Y value for this point
            percentY = (points[this.displayOffset+i] - minValue)/range;
            int newY = (int)Math.round(h - Math.abs(h*percentY));
            // figure out the X value for this point
            int newX = (int)Math.round(xInc*i + X_OFFSET);

            // fill in this point
            big.fillOval(newX-(POINT_SIZE/2), newY-(POINT_SIZE/2),
                         POINT_SIZE, POINT_SIZE);
            // draw a line from the last point to this one
            big.drawLine(oldX, oldY, newX, newY);

            // save this point as the old one for the next point
            oldX = newX;
            oldY = newY;
         }

         // set the last index drawn to include all the current points
         lastIndexDrawn = pointIndex;
      }

      // blot the off-screen image onto the screen graphics
      g2.drawImage(imgTemp, 0, 0, this);
   }*/

   public void update(Graphics g)
   {
      BufferedImage imgTemp = this.img;
      // need Graphics2D to set the stroke
      Graphics2D g2 = (Graphics2D)g;

      // get the current size of the panel
      Dimension d = getSize();
      //int width = d.width - X_OFFSET;
      //int height = d.height - Y_BUFFER;
      int width = d.width;
      int height = d.height;

      //int stretchWidth = pointIndex*POINT_SIZE;
      //if(stretchWidth>width)
      //{
      //   this.setPreferredSize(new Dimension(stretchWidth, height));
      //   d.width = stretchWidth;
      //   width = stretchWidth - X_OFFSET;
      //}

      int spacing = (int)((width/(double)pointIndex)+0.5);
      //double xInc = ((double)width)/points.length;
      double xInc = ((double)width-X_OFFSET)/points.length;
      //double xInc = ((double)width-X_OFFSET)/this.pointIndex;

      // check to see if we need to recreate our off-screen image
      if(imgTemp==null || this.currentSize==null || !d.equals(currentSize))
      {
         // create the buffered image
         imgTemp = (BufferedImage)createImage(width, height);
         // save the size of the image
         this.currentSize = d;
         // get the graphics object and initialize
         Graphics2D big = imgTemp.createGraphics();

         big.setStroke(normal);
         // paint the background white
         big.setColor(Color.white);
         big.fillRect(0,0,d.width,d.height);

         // draw the X-Axis tick marks
         big.setStroke(dashed); // Set this line stroking style

         boolean xMajorTick = false;
         boolean upDown = false;
         int x = X_OFFSET;
         for(int i=0; x+i<width; i += 50)
         {
            if(xMajorTick=!xMajorTick)
               big.setColor(Color.darkGray);
            else
               big.setColor(Color.darkGray);
            big.drawLine(x+i, 0, x+i, height - X_OFFSET + 10);

            if(i<pointsLabel.length && pointsLabel[i]!=null)
            {
               big.setColor(Color.blue);
               if( upDown=!upDown )
                  big.drawString(pointsLabel[i], (x+i)-10, height+Y_BUFFER/2);
               else
                  big.drawString(pointsLabel[i], (x+i)-10, height+Y_BUFFER);
            }
         }
         big.drawString("Right-Click on Graph for more options", 
            Y_BUFFER*2, height - X_OFFSET + 25);

         // draw the Y-Axis tick marks
         boolean dark = false;
         double numLines = (height/Y_GRID_RATE)+1;
         for(int i=1; i<numLines-1; i++)
         {
            if(dark=!dark) // major tick (intentional assignment)
              big.setColor(Color.darkGray);
            else // minor tick
              big.setColor(Color.lightGray);
            big.drawLine(0, i*Y_GRID_RATE, width, i*Y_GRID_RATE);
            String lbl = nf.format(this.max - (i/(numLines-1))*this.range);
            big.setColor(Color.blue);
            big.drawString(lbl,0,i*Y_GRID_RATE);
         }

         // reset the last index drawn, since none have been drawn yet
         this.lastIndexDrawn = -1;
         this.img = imgTemp;
      }

      // check to see if there are any new points to draw
      if(pointIndex>0 && pointIndex!=lastIndexDrawn)
      {
         // calculate the starting point
         int i = (lastIndexDrawn<0)?0:lastIndexDrawn-1;
         double minValue = this.min;
         // figure out the Y value for the starting point
         double percentY = (points[i] - minValue)/range;
         int oldY = (int)Math.round(height - Math.abs(height*percentY));
         // figure out  the X value for the starting point
         int oldX = (int)Math.round(xInc*i + X_OFFSET);
         // get the Graphics2D object
         Graphics2D big = (Graphics2D)imgTemp.getGraphics();
         big.setColor(Color.blue);
         big.setStroke(normal);

         // if this is the first point drawn, it needs to be filled
         if(lastIndexDrawn<0)
            big.fillOval(oldX-(POINT_SIZE/2), oldY-(POINT_SIZE/2),
                          POINT_SIZE, POINT_SIZE);

         // go ahead and advance i to the next point
         i++;
         // now draw the rest of the points
         for(; i<pointIndex; i++)
         {
            // figure out the Y value for this point
            percentY = (points[this.displayOffset+i] - minValue)/range;
            int newY = (int)Math.round(height - Math.abs(height*percentY));
            // figure out the X value for this point
            int newX = (int)Math.round(xInc*i + X_OFFSET);

            // fill in this point
            big.fillOval(newX-(POINT_SIZE/2), newY-(POINT_SIZE/2),
                         POINT_SIZE, POINT_SIZE);
            // draw a line from the last point to this one
            big.drawLine(oldX, oldY, newX, newY);

            // save this point as the old one for the next point
            oldX = newX;
            oldY = newY;
         }

         // set the last index drawn to include all the current points
         lastIndexDrawn = pointIndex;
      }

      // blot the off-screen image onto the screen graphics
      g2.drawImage(imgTemp, 0, 0, this);
   }

   //private static final int LABEL_OFFSET = 40;

   /*private void valueToScreenCoord(int index, Point screenCoord)
   {

      double percentY = (this.points[index]-this.min)/this.range;
      double percentX = 0;

      //screenCoord.x = (index*spacing) + LABEL_OFFSET;
      //screenCoord.y = ((int)Math.round(percentY*height));
   }*/

   public void zoomFull()
   {
      // get the current size of the panel
      Dimension d = getSize();
      int width = d.width - X_OFFSET;
      int height = d.height - Y_BUFFER;

      //int stretchWidth = pointIndex;
      //if(stretchWidth>width)
      //{
      //   this.setPreferredSize(new Dimension(stretchWidth, height));
      //   d.width = stretchWidth;
      //   width = stretchWidth - X_OFFSET;
      //}

      double max = this.points[0];
      double min = this.points[0];
      for(int i=0; i<pointIndex; i++)
      {
         if(max<this.points[i])
            max = this.points[i];
         if(min>this.points[i])
            min = this.points[i];
      }
      double range = Math.abs(max-min);
      double fluff = range*.10;
      setScale(min-fluff, max+fluff);

   }


   private void maybeShowPopup(MouseEvent e)
   {
       if (e.isPopupTrigger()) 
       {
           data.show(e.getComponent(),
                      e.getX(), e.getY());
       }
   }
   public void mousePressed(MouseEvent e)
   {
       maybeShowPopup(e);
   }
   public void mouseReleased(MouseEvent e)
   {
       maybeShowPopup(e);
   }
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
    */
   public void mouseClicked(MouseEvent e)
   {}
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
    */
   public void mouseEntered(MouseEvent e)
   {}
   /* (non-Javadoc)
    * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
    */
   public void mouseExited(MouseEvent e)
   {}
}
