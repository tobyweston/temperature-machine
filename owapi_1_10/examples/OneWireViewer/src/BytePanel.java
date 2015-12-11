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
import java.util.*;

import com.dalsemi.onewire.utils.Convert;

/**
 * A panel for displaying an array of bytes as a hex-encoded string
 * in a series of JTextFieds.  The BytePanel allows editing of the
 * hex-encoded strings and will return the updated byte[].
 *
 * @author SH
 * @version 1.00
 */
public class BytePanel extends JPanel
{
   private static final int BYTES_PER_ROW = 16;
   private byte[] bytes = null;
   private JTable table = null;
   private boolean hasChanged = false;
   private Vector elements = new Vector();
   private int bytesPerRow = BYTES_PER_ROW;

   /**
    * KeyAdapter for tracking updates to JTextFields.
    */
   private KeyAdapter ka = new KeyAdapter()
   {
      public void keyTyped(KeyEvent e)
      {
         if(!e.isControlDown())
         {
            ((JTextField)e.getSource()).setBackground(Color.yellow);
            hasChanged = true;
         }
      }
   };

   /**
    * Returns true if the contents of any of the JTextFields has changed.
    *
    * @return true if the contents of any of the JTextFields has changed.
    */
   public boolean hasChanged()
   {
      return hasChanged;
   }

   /**
    * Converts the hex-encoded string back into a byte[] and returns the
    * updated byte[].
    *
    * @return the updated byte[].
    */
   public synchronized byte[] getBytes(int expectedCount)
      throws Convert.ConvertException
   {
      if(!hasChanged)
         return bytes;
      
      int byteCount = bytes.length;

      Vector l_elements = elements;
      int elem_size = l_elements.size();
      if(elem_size>0)
      {
         byte[] data = new byte[bytesPerRow];
         for(int i=0; i<elem_size; i++)
         {
            int index = i*bytesPerRow;
            JTextField text = (JTextField)l_elements.elementAt(i);
            if(text.getBackground()==Color.yellow)
            {
               String strBytes = text.getText();
               int len = Convert.toByteArray(strBytes, data, 0, data.length);

               byteCount -= (data.length - len);
               System.arraycopy(data, 0, bytes, index,
                                Math.min(len, bytes.length-index));
               text.setBackground(Color.white);
            }
         }
      }

      if(expectedCount>0 && expectedCount!=byteCount)
      {
         Object[] options = { "OK", "CANCEL" };
         int result = JOptionPane.showOptionDialog(null, 
            "Wrong number of bytes: expected " + expectedCount +
            ", got " + byteCount + "\r\n\r\nClick OK to pad with zeroes.", 
            "Hex Editor Panel Error", 
            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
            null, options, options[0]);

         if(result>0)
            return null;
      }
      return bytes;
   }

   /**
    * Sets the bytes displayed in this byte panel.
    *
    * @param b the bytes to be displayed.
    */
   public void setBytes(byte[] b)
   {
      setBytes(b,0,b.length);
   }

   /**
    * Sets the bytes displayed in this byte panel.
    *
    * @param b the bytes to be displayed.
    * @param off the offset into the byte array to start.
    * @param len the number of bytes to display.
    */
   public void setBytes(byte[] b, int off, int len)
   {
      setBytes(BYTES_PER_ROW, b, off, len);
   }

   /**
    * Sets the bytes displayed in this byte panel with the
    * provided value for number of bytes per row.
    *
    * @param numPerRow the number of bytes displayed in each JTextField
    * @param b the bytes to be displayed.
    * @param off the offset into the byte array to start.
    * @param len the number of bytes to display.
    */
   public void setBytes(int numPerRow, byte[] b, int off, int len)
   {
      clearBytes();
      bytes = new byte[len];
      System.arraycopy(b,0,bytes,0,len);
      elements.removeAllElements();

      this.bytesPerRow = numPerRow;
      int cols = Math.min(len, numPerRow);
      int rows = (len/numPerRow) + (len%numPerRow>0?1:0);

      JPanel labeledBytes = new JPanel();
      labeledBytes.setLayout(new BoxLayout(labeledBytes, BoxLayout.Y_AXIS));
      this.add(labeledBytes);

      StringBuffer value = new StringBuffer(cols*3);
      JPanel panel = new JPanel();
      JPanel gridPanel = new JPanel(new GridLayout(rows,1,2,2));
      for(int i=0; i<rows; i++)
      {
         value.delete(0, value.length());
         int start = i*cols;
         String hexValue = Convert.toHexString(b, start + off,
                                             Math.min(cols, len-start), " ");
         JTextField text = new JTextField(hexValue, numPerRow*3);
         text.addKeyListener(ka);
         text.setFont(Viewer.fontBold);
         gridPanel.add(text);
         elements.add(text);
         panel.add(gridPanel, BorderLayout.SOUTH);
      }
      labeledBytes.add(panel);
      this.setVisible(true);
   }

   /**
    * Sets the bytes displayed in this byte panel with the
    * provided value for number of bytes per row.
    *
    * @param labels labels to print before each group of rows.
    * @param labelRate number of rows per label (i.e. how many JTextFields are
    *        in a group that share a label).
    * @param numPerRow the number of bytes displayed in each JTextField
    * @param b the bytes to be displayed.
    * @param off the offset into the byte array to start.
    * @param len the number of bytes to display.
    */
   public synchronized void setBytes(String[] labels, int labelRate,
                                     int numPerRow, byte[] b, int off, int len)
   {
      clearBytes();
      bytes = new byte[len];
      System.arraycopy(b,0,bytes,0,len);
      elements.removeAllElements();

      this.bytesPerRow = numPerRow;
      int cols = Math.min(len, numPerRow);
      int rows = (len/numPerRow) + (len%numPerRow>0?1:0);
      int lblCount = labels.length;

      JPanel labeledBytes = new JPanel();
      labeledBytes.setLayout(new BoxLayout(labeledBytes, BoxLayout.Y_AXIS));
      this.add(labeledBytes);

      for(int i=0; i<rows;)
      {
         JPanel panel = new JPanel(new BorderLayout());
         JLabel label = new JLabel(labels[(i/labelRate)%lblCount]);
         panel.add(label, BorderLayout.NORTH);
         JPanel gridPanel = new JPanel(new GridLayout(labelRate,1,2,2));
         for(int j=0; j<labelRate && i<rows; j++, i++)
         {
            int start = i*cols;
            String hexValue = Convert.toHexString(b, start + off,
                                                Math.min(cols, len-start), " ");
            JTextField text = new JTextField(hexValue, numPerRow*3);
            text.addKeyListener(ka);
            text.setFont(Viewer.fontBold);
            gridPanel.add(text);
            elements.add(text);
         }
         panel.add(gridPanel, BorderLayout.SOUTH);
         labeledBytes.add(panel);
      }
      this.setVisible(true);
   }

   /**
    * erases all bytes and clears the display area.
    */
   public synchronized void clearBytes()
   {
      this.removeAll();
      this.invalidate();
      this.setVisible(false);
      this.bytes = null;
      this.hasChanged = false;
   }
}
