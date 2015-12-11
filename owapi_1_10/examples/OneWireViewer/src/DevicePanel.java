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
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import java.util.*;

import com.dalsemi.onewire.application.monitor.*;
import com.dalsemi.onewire.application.tag.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.adapter.*;

/**
 * A panel displaying a list of 1-Wire devices.
 *
 * @author SH
 * @version 1.0
 */
public final class DevicePanel
   extends JPanel
   implements DeviceMonitorEventListener
{

   private Object syncFlag = new Object();

   private JList list = null;
   private DefaultListModel listModel = null;;
   private MessageLog log = null;

   private JLabel lblDeviceCount = null, lblAdapter = null;
   private int deviceCount = 0;
   private int errorCount = 0;

   private String lastErrorReported = null;

   private boolean useAlternateNames = false;

   /**
    * Constructs a new device list using the given adapter and message log.
    *
    * @param adapter the DSPortAdapter which will be monitored for devices.
    * @param log the MessageLog for logging errors.
    */
   public DevicePanel(MessageLog log)
   {
      this(log, false);
   }

   /**
    * Constructs a new device list using the given adapter and message log.
    *
    * @param adapter the DSPortAdapter which will be monitored for devices.
    * @param log the MessageLog for logging errors.
    * @param multiple indicates that the list should allow multiple selections.
    */
   public DevicePanel(MessageLog log, boolean multiple)
   {
      super(new BorderLayout());

      setLogger(log);

      JLabel lblDeviceList = new JLabel("        Device List        ");
      lblDeviceList.setFont(Viewer.fontBold);

      this.listModel = new DefaultListModel();
      this.list = new JList(listModel);
      this.list.setSelectionMode( (multiple ?
                 ListSelectionModel.MULTIPLE_INTERVAL_SELECTION :
                 ListSelectionModel.SINGLE_SELECTION) );
      this.list.setFont(Viewer.fontPlain);
      this.list.setCellRenderer(new DeviceListRenderer(this));
      JScrollPane scrollPane = new JScrollPane(this.list);

      JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
      this.lblDeviceCount = new JLabel(" 0 Devices");
      this.lblDeviceCount.setFont(Viewer.fontBold);
      this.lblAdapter = new JLabel();
      this.lblAdapter.setFont(Viewer.fontBold);
      labelPanel.add(this.lblDeviceCount);
      labelPanel.add(this.lblAdapter);

      add(lblDeviceList, BorderLayout.NORTH);
      add(scrollPane, BorderLayout.CENTER);
      add(labelPanel, BorderLayout.SOUTH);
   }

   /**
    * Returns the listModel for this DevicePanel's JList
    */
    public DefaultListModel getListModel()
    {
       return this.listModel;
    }

   /**
    * Sets the adapter label, displayed below the list of devices.  Used
    * to indicate which adapter these devices were found on.
    *
    * @param adapterString string used to identify the adapter the current
    *        list of devices were found on.
    */
   public void setAdapterLabel(String adapterString)
   {
      if(adapterString.length()>16)
         lblAdapter.setText(adapterString.substring(0, 13) + "...");
      else
         lblAdapter.setText(adapterString);
      lblAdapter.setToolTipText(adapterString);
   }

   /**
    * Adds a listener to the list that's notified each time a change
    * to the selection occurs.
    *
    * @param listener the <code>ListSelectionListener</code> to add
    */
   public void addListSelectionListener(ListSelectionListener lsl)
   {
      list.addListSelectionListener(lsl);
   }

   /**
    * Removes a listener from the list that's notified each time a
    * change to the selection occurs.
    *
    * @param listener the <code>ListSelectionListener</code> to remove
    */
   public void removeListSelectionListener(ListSelectionListener listener)
   {
      listenerList.remove(ListSelectionListener.class, listener);
   }

   /**
    * Add a listener to the list that's notified each time a change
    * to the devices available in the list occurs.
    *
    * @param l the ListDataListener
    */
   public void addListDataListener(ListDataListener ldl)
   {
      this.listModel.addListDataListener(ldl);
   }

   /**
    * Remove a listener from the list that's notified each time a
    * change to the devices available in the list occurs.
    * @param l the ListDataListener
    */
   public void removeListDataListener(ListDataListener ldl)
   {
      this.listModel.removeListDataListener(ldl);
   }

   /**
    * Returns true if nothing is selected.
    *
    * @return true if nothing is selected
    */
   public boolean isSelectionEmpty()
   {
      return list.isSelectionEmpty();
   }

   /**
    * Returns true if the device list is empty.
    *
    * @return true if the device list is empty
    */
   public boolean isListEmpty()
   {
      return listModel.getSize()==0;
   }

   /**
    * Returns the index of the currently selected device.
    *
    * @return the index of the currently selected device.
    */
   public int getSelectedIndex()
   {
      return list.getSelectedIndex();
   }


   /**
    * Sets the currently selected device.  If the device isn't in the
    * list, nothing happens.
    *
    * @param owc the device to set as the selected device in the list.
    */
   public void setSelectedContainer(OneWireContainer owc)
   {
      int i = this.listModel.indexOf(owc);
      if (i>=0)
      {
         list.setSelectedIndex(i);
      }
   }

   /**
    * Returns the currently selected device.
    *
    * @return the currently selected device in the list.
    */
   public OneWireContainer getSelectedContainer()
   {
      if(list.isSelectionEmpty())
         return null;

      return (OneWireContainer)list.getSelectedValue();
   }

   /**
    * Returns an array of the currently selected devices.  Will only
    * be 1 device if multiple selection is not enabled.
    *
    * @return an array of the currently selected devices.
    */
   public OneWireContainer[] getSelectedContainers()
   {
      Object[] oArray = list.getSelectedValues();
      java.util.ArrayList list;

      if(oArray==null)
         return null;

      OneWireContainer[] owcs = new OneWireContainer[oArray.length];
      for(int i=0; i<oArray.length; i++)
         owcs[i] = (OneWireContainer)oArray[i];

      return owcs;
   }

   /**
    * Sets the currently selected device.  If the device isn't in the
    * list, nothing happens.
    *
    * @param address the address of the device to set as the selected device
    * in the list.
    */
   public void setSelectedAddress(String address)
   {
      int index = -1;
      for(int i=0; index<0 && i<listModel.size(); i++)
      {
         OneWireContainer owc = (OneWireContainer)listModel.elementAt(i);
         if(owc.getAddressAsString().equals(address))
            index = i;
      }
      if (index>=0)
      {
         list.setSelectedIndex(index);
      }
   }

   /**
    * Returns the address of currently selected device.
    *
    * @return the address of currently selected device in the list.
    */
   public String getSelectedAddress()
   {
      return ((OneWireContainer)list.getSelectedValue()).getAddressAsString();
   }

   /**
    * Returns an array of the addresses of the currently selected devices.
    * Will only be 1 device if multiple selection is not enabled.
    *
    * @return an array of the addresses of the currently selected devices.
    */
   public String[] getSelectedAddresses()
   {
      Object[] oArray = list.getSelectedValues();
      if(oArray==null)
         return null;

      String[] addrs = new String[oArray.length];
      for(int i=0; i<oArray.length; i++)
         addrs[i] = ((OneWireContainer)oArray[i]).getAddressAsString();

      return addrs;
   }

   /**
    * Returns all containers in this device list
    */
   public OneWireContainer[] getAllContainers()
   {
      Object[] oArray = listModel.toArray();
      if(oArray==null)
         return null;

      OneWireContainer[] owcs = new OneWireContainer[oArray.length];
      for(int i=0; i<oArray.length; i++)
         owcs[i] = (OneWireContainer)oArray[i];

      return owcs;
   }

   /**
    * Sets the MessageLog for the device list.
    *
    * @param log the MessageLog for the device list.
    */
   public void setLogger(MessageLog log)
   {
      this.log = log;
   }

   /**
    * Displays an exception in a popup dialog window and pauses the monitor.
    */
   protected void displayException(Exception e)
   {
      JOptionPane.showMessageDialog(this.getParent().getParent(),
         "Exception Occurred During 1-Wire Search:\n" +e.getMessage(),
         "Exception Occurred", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
   }

   /**
    * Arrival event as a NetworkMonitorEventListener
    *
    * @param nme NetworkMonitorEvent add
    */
   public void deviceArrival (DeviceMonitorEvent dme)
   {
      int cnt = dme.getDeviceCount();
      for(int i=0; i<cnt; i++)
         listModel.addElement(dme.getContainerAt(i));

      deviceCount += cnt;
      updateDeviceLabel();
   }

   /**
    * Depart event as a NetworkMonitorEventListener
    *
    * @param nme NetworkMonitorEvent depart
    */
   public void deviceDeparture (DeviceMonitorEvent dme)
   {
      int cnt = dme.getDeviceCount();
      for(int i=0; i<cnt; i++)
         listModel.removeElement(dme.getContainerAt(i));

      deviceCount -= cnt;
      updateDeviceLabel();
   }

   public void updateDeviceLabel()
   {
      if(listModel.getSize()==1)
         this.lblDeviceCount.setText(" 1 Device");
      else
         this.lblDeviceCount.setText(" " + listModel.getSize() + " Devices");
   }

   /**
    * Exception event as a NetworkMonitorEventListener
    *
    * @param ex Exception
    */
   public void networkException (DeviceMonitorException e)
   {
      log.addError("DevicePanel", null, e.toString());
      //JOptionPane.showMessageDialog(this.getParent().getParent().getParent(),
      //   "Exception Occurred During 1-Wire Search:\n" +e.toString(),
      //   "Exception Occurred", JOptionPane.ERROR_MESSAGE);
   }

   public void setUseAlternateNames(boolean useAlternate)
   {
      this.useAlternateNames = useAlternate;
   }

   public boolean getUseAlternateNames()
   {
      return this.useAlternateNames;
   }

}

class DeviceListRenderer implements ListCellRenderer
{
   DevicePanel dp;

   public DeviceListRenderer(DevicePanel dp)
   {
      this.dp = dp;
   }

   public Component getListCellRendererComponent(JList list, Object value,
       int index, boolean isSelected, boolean cellHasFocus)
   {
      JLabel label;
      // added the following block so that the name of the owc41 device will
      // show up properly in the device list.
//      if(value instanceof com.dalsemi.onewire.container.OneWireContainer41)
//      {
//         OneWireContainer41 l_owc41
//            = (OneWireContainer41)value;
//         DSPortAdapter l_adapter = l_owc41.getAdapter();
//         try
//         {
//            l_adapter.beginExclusive(true);
//            // read the device config byte just to force the init of 
//            // description and other parameters
//            byte configByte = l_owc41.getDeviceConfigByte();
//         }
//         catch ( Exception e )
//         {}
//         finally
//         {
//            l_adapter.endExclusive();
//         }
//      }
      
      if((value instanceof OneWireContainer) && dp.getUseAlternateNames())
      {
         OneWireContainer owc = (OneWireContainer)value;
         

         label = new JLabel(
                    owc.getAddressAsString() + " " + owc.getAlternateNames(),
                    JLabel.LEFT);
      }
      else
         label = new JLabel(value.toString(), JLabel.LEFT);
      label.setFont(Viewer.fontPlain);
      label.setOpaque(true);
      if (isSelected)
      {
         label.setBackground(list.getSelectionBackground());
         if(value instanceof TaggedDevice)
            label.setForeground(Color.blue);
         else
            label.setForeground(list.getSelectionForeground());
      }
      else
      {
         label.setBackground(list.getBackground());
         if(value instanceof TaggedDevice)
            label.setForeground(Color.blue);
         else
            label.setForeground(list.getForeground());
      }


      return label;
   }
}


class AdapterNode extends DefaultMutableTreeNode
{
   DSPortAdapter adapter;
   String toString;

   public AdapterNode(DSPortAdapter adapter)
   {
      super(adapter, true);
      this.adapter = adapter;
      this.toString = adapter.toString();
   }

   /**
    * returns true if this file node has no children.
    * @return true if this file node has no children.
    */
   public boolean isLeaf()
   {
      return false;
   }

   /**
    * overrides default getChildAt for null
    * check.
    */
   public TreeNode getChildAt(int index)
   {
      if(super.children==null)
         return null;
      return super.getChildAt(index);
   }


   /**
    * Returns the OWFile this file node holds.
    * @return the OWFile this file node holds.
    */
   public DSPortAdapter getAdapter()
   {
      return this.adapter;
   }

   /**
    * overrides default setUserObject for caching
    * certain values.
    * @param f the OWFile this file node will hold.
    */
   public void setUserObject(DSPortAdapter adapter)
   {
      super.setUserObject(adapter);
      this.adapter = adapter;
      this.toString = adapter.toString();
      this.removeAllChildren();
   }

   /**
    * Returns the name of the file and it's length.
    *
    * @return the name of the file and it's length.
    */
   public String toString()
   {
      return toString;
   }

   /**
    * If the provided tree node is a child of this node,
    * its index is returned.
    *
    * @param aChild the child to search for
    * @return the index of the child
    */
   public int getIndex(TreeNode aChild)
   {
      if (aChild == null || children==null)
         return -1;
      return children.indexOf(aChild);	// linear search
   }

   /**
    * Checks to see if two file nodes are equivalent
    *
    * @param o the object to test for equivalence
    * @return true if the object is a file node
    *         holding the same file.
    */
   public boolean equals(Object o)
   {
      if(!(o instanceof AdapterNode))
         return false;

      AdapterNode n = (AdapterNode)o;

      if(n.adapter==null)
         return this.adapter==null;

      return n.adapter.equals(this.adapter);
   }

}

/**
 * class for all entries in the JList, representing an entry which holds
 * a OneWireContainer, tests for equivalence based on the 1-Wire address,
 * and caches the value of the toString.
 */
class DeviceEntry
{
   OneWireContainer owc;
   String name, address, toString;
   int stateCount;

   /**
    * Creates an empty device entry.  must call <code>setContainer(owc)</code>
    */
   public DeviceEntry()
   {}

   /**
    * Creates a device entry with the provided OneWireContainer.
    */
   public DeviceEntry(OneWireContainer owc)
   {
      setContainer(owc);
   }

   /**
    * Sets the container for this Device Entry.
    *
    * @param owc OneWireContainer of this Device Entry
    */
   public void setContainer(OneWireContainer owc)
   {
      this.owc = owc;
      this.name = owc.getName();
      this.address = owc.getAddressAsString();
      this.toString = this.address + " " + this.name;
   }

   /**
    * Sets the container for this Device Entry.
    *
    * @param owc OneWireContainer of this Device Entry
    */
   public OneWireContainer getContainer()
   {
      return this.owc;
   }

   /**
    * Returns the current value of the state count, used to keep track
    * of how long the device has been missing from searches.
    *
    * @return current value of state count
    */
   public int getStateCount()
   {
      return stateCount;
   }

   /**
    * Sets the current value of the state count, used to keep track
    * of how long the device has been missing from searches.
    *
    * @param cnt current value of state count
    */
   public void setStateCount(int cnt)
   {
      stateCount = cnt;
   }

   /**
    * Returns a descriptive string including address and DS part number.
    *
    * @return a descriptive string including address and DS part number.
    */
   public String toString()
   {
     return this.toString;
   }

   /**
    * Returns true if object is a device entry and has a container with
    * the same address as this device entry.
    *
    * @param o Object to be compared
    * @return <code>true</code> if the object is the same
    */
   public boolean equals(Object o)
   {
      if(o instanceof DeviceEntry)
      {
         String addr = ((DeviceEntry)o).address;
         if(addr==null||this.address==null)
            return addr==null&&this.address==null;
         else
            return addr.equals(this.address);
      }

      return false;
   }
}

class SwitchEntry extends DeviceEntry implements MutableTreeNode
{
   JList[] channels;

   /**
    * Creates an empty switch entry.  must call <code>setContainer(owc)</code>
    */
   public SwitchEntry()
   {
      this.channels = new JList[0];
   }

   /**
    * Creates a switch entry with the provided OneWireContainer.
    */
   public SwitchEntry(OneWireContainer owc, int numChannels)
   {
      setContainer(owc);
      setNumberOfChannels(numChannels);
   }

   /**
    * Sets the container for this Device Entry.
    *
    * @param owc OneWireContainer of this Device Entry
    */
   public void setContainer(OneWireContainer owc)
   {
      super.owc = owc;
      super.name = owc.getName();
      super.address = owc.getAddressAsString();
      super.toString = address + " " + name;
   }

   public void setNumberOfChannels(int numChannels)
   {
      this.channels = new JList[numChannels];
      for(int i=0; i<numChannels; i++)
         this.channels[i] = new JList(new Object[] {"Channel " + (char)('A'+i)});
   }

   public int getNumberOfChannels()
   {
      return this.channels.length;
   }


   /**
    * Adds <code>child</code> to the receiver at <code>index</code>.
    * <code>child</code> will be messaged with <code>setParent</code>.
    */
   public void insert(MutableTreeNode child, int index)
   {
   }

   /**
    * Removes the child at <code>index</code> from the receiver.
    */
   public void remove(int index)
   {
   }

   /**
    * Removes <code>node</code> from the receiver. <code>setParent</code>
    * will be messaged on <code>node</code>.
    */
   public void remove(MutableTreeNode node)
   {
   }

   /**
    * Resets the user object of the receiver to <code>object</code>.
    */
   public void setUserObject(Object object)
   {

   }

   /**
    * Removes the receiver from its parent.
    */
   public void removeFromParent()
   {
   }

   /**
    * Sets the parent of the receiver to <code>newParent</code>.
    */
   public void setParent(MutableTreeNode newParent)
   {
   }

   public Enumeration children()
   {
      Vector v = new Vector();
      for(int i=0; i<channels.length; i++)
      {
         v.addElement(channels[i]);
      }
      return v.elements();
   }

   /**
    * Returns the child <code>TreeNode</code> at index
    * <code>childIndex</code>.
    */
   public TreeNode getChildAt(int childIndex)
   {
      return new DefaultMutableTreeNode(channels[childIndex]);
   }

   /**
    * Returns the number of children <code>TreeNode</code>s the receiver
    * contains.
    */
   public int getChildCount()
   {
      return getNumberOfChannels();
   }

   /**
    * Returns the parent <code>TreeNode</code> of the receiver.
    */
   public TreeNode getParent()
   {
      return null;
   }

   /**
    * Returns the index of <code>node</code> in the receivers children.
    * If the receiver does not contain <code>node</code>, -1 will be
    * returned.
    */
   public int getIndex(TreeNode node)
   {
      for(int i=0; i<channels.length; i++)
         if(((DefaultMutableTreeNode)node).getUserObject().equals(channels[i]))
            return i;

      return -1;
   }

   /**
    * Returns true if the receiver allows children.
    */
   public boolean getAllowsChildren()
   {
      return true;
   }

   /**
    * Returns true if the receiver is a leaf.
    */
   public boolean isLeaf()
   {
      return false;
   }

}



