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
import java.awt.event.*;
import javax.swing.*;
import java.util.*;

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;

/**
 * A graphical Adapter Chooser panel for displaying in a dialog.  Allows
 * the user to select (and permanently set) the default <code>DSPortAdapter</code>
 * used by OneWireViewer.  The entire list of all adapters supported on the
 * system will be selectable, as well as all available ports.
 *
 * @author SH
 * @version 1.00
 */
public class AdapterChooser extends JPanel
{
   private static final String selectString = "Please Select Port";
   private String defaultPortProp, defaultAdapterProp;
   private JTextField defaultPort, defaultAdapter;
   private JButton loadAdapter = null;
   private JPanel defaultPanel = null;
   private Font textFontBold = null, textFontPlain = null;
   private JTabbedPane tabbedPane = null;

   /**
    * Constructs a new adapter chooser, ready for display.
    */
   public AdapterChooser(String defaultAdapterName,
                         String defaultAdapterPort,
                         Font textFontBold, Font textFontPlain)
   {
      this.textFontBold = textFontBold;
      this.textFontPlain = textFontPlain;
      this.defaultPortProp = defaultAdapterPort;
      this.defaultAdapterProp = defaultAdapterName;

      this.tabbedPane = new JTabbedPane();

      getPortSelection();

      defaultPanel = new JPanel(new GridLayout(2, 1, 3, 15));
      //defaultPanel.setLayout(new BoxLayout(defaultPanel, BoxLayout.Y_AXIS));
      defaultPanel.setBorder(BorderFactory.createTitledBorder(
         BorderFactory.createEtchedBorder(), "Default Port"));

      JPanel adapterPanel = new JPanel(new GridLayout(2,2,3,3));
      JLabel lblAdapterName = new JLabel("Adapter Name", JLabel.RIGHT);
      lblAdapterName.setFont(textFontBold);
      lblAdapterName.setOpaque(true);
      lblAdapterName.setBackground(Color.lightGray);
      lblAdapterName.setForeground(Color.black);
      adapterPanel.add(lblAdapterName);

      defaultAdapter = new JTextField(defaultAdapterName);
      adapterPanel.add(defaultAdapter);

      JLabel lblAdapterPort = new JLabel("Adapter Port", JLabel.RIGHT);
      lblAdapterPort.setFont(textFontBold);
      lblAdapterPort.setOpaque(true);
      lblAdapterPort.setBackground(Color.lightGray);
      lblAdapterPort.setForeground(Color.black);
      adapterPanel.add(lblAdapterPort);

      defaultPort = new JTextField(defaultAdapterPort);
      adapterPanel.add(defaultPort);

      defaultPanel.add(adapterPanel);

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      JButton refresh = new JButton("Refresh Adapter List");
      refresh.addActionListener(new ActionListener()
         {
            public void actionPerformed(ActionEvent ae)
            {
               getPortSelection();
            }
         }
      );

      buttonPanel.add(refresh);
      defaultPanel.add(buttonPanel);

      this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      this.add(tabbedPane);
      this.add(defaultPanel);
   }

   /**
    * Adds all adapters and ports to the provided JTabbedPane
    */
   private void getPortSelection()
   {
      tabbedPane.removeAll();
      
      boolean tmexAdaptersExist = false;
      Vector vAdapters = new Vector();
      Enumeration adapters = OneWireAccessProvider.enumerateAllAdapters();
      while(adapters.hasMoreElements())
      {
         Object o = adapters.nextElement();
         if(o instanceof com.dalsemi.onewire.adapter.TMEXAdapter)
            tmexAdaptersExist = true;
         vAdapters.addElement(o);
      }
      
      for(int i=0; i<vAdapters.size(); i++)
      {
         DSPortAdapter adapter = (DSPortAdapter)vAdapters.elementAt(i);
         final String adapterName = adapter.getAdapterName();
         if(adapterName.equals("DS9097U") && tmexAdaptersExist)
         {
            // don't show pure-serial adapter if TMEX is available,
            // unless property is set to override
            if(!ViewerProperties.getPropertyBoolean(
                     OneWireViewer.ALLOW_PURE_SERIAL_ADAPTER, false))
               continue;
         }

         // info panel
         JPanel infoPanel = new JPanel(new GridLayout(3, 2, 3, 3));
         infoPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), "Port Information"));

         JLabel lblPortTypeHdr = new JLabel("Port Type", JLabel.RIGHT);
         lblPortTypeHdr.setOpaque(true);
         lblPortTypeHdr.setFont(textFontBold);
         lblPortTypeHdr.setForeground(Color.black);
         lblPortTypeHdr.setBackground(Color.lightGray);

         JLabel lblPortType = new JLabel(adapter.getPortTypeDescription());
         lblPortType.setOpaque(true);
         lblPortType.setFont(textFontPlain);
         lblPortType.setForeground(Color.black);
         lblPortType.setBackground(Color.lightGray);

         infoPanel.add(lblPortTypeHdr);
         infoPanel.add(lblPortType);

         JLabel lblPort = new JLabel("Select Port ", JLabel.RIGHT);
         lblPort.setFont(textFontBold);
         lblPort.setOpaque(true);
         lblPort.setBackground(Color.lightGray);
         lblPort.setForeground(Color.black);
         infoPanel.add(lblPort);

         JPanel portPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
         portPanel.setBackground(Color.lightGray);
         JComboBox portList = new JComboBox();
         portList.setFont(textFontPlain);
         portList.addItem(selectString);
         portList.addActionListener(new ActionListener()
            {
               public void actionPerformed(ActionEvent ae)
               {
                  String s
                     = ((JComboBox)ae.getSource()).getSelectedItem().toString();

                  defaultAdapter.setText(adapterName);
                  if(!s.equals(selectString))
                     defaultPort.setText(s);
                  else
                     defaultPort.setText("");
               }
            }
         );

         portPanel.add(portList);
         infoPanel.add(portPanel);

         Enumeration ports = adapter.getPortNames();
         while(ports.hasMoreElements())
         {
            portList.addItem(ports.nextElement());
         }

         tabbedPane.addTab(adapterName, infoPanel);
         if(adapterName.equals(defaultAdapterProp))
            tabbedPane.setSelectedComponent(infoPanel);
      }
   }

   /**
    * Returns the name of the adpater selected by the user.
    * @return the name of the adpater selected by the user.
    */
   public String getAdapterName()
   {
      return defaultAdapter.getText();
   }

   /**
    * Returns the port of the adapter selected by the user.
    * @return the port of the adapter selected by the user.
    */
   public String getAdapterPort()
   {
      return defaultPort.getText();
   }
}
