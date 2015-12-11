import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.swing.*;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.monitor.*;
import com.dalsemi.onewire.application.tag.TaggedDevice;
import com.dalsemi.onewire.application.tag.TAGParser;
import com.dalsemi.onewire.application.file.OWFileInputStream;
import com.dalsemi.onewire.application.file.OWFile;
import com.dalsemi.onewire.utils.OWPath;

public class TagMonitor
   implements DeviceMonitorEventListener
{
   /** devicePanel, displayed in OneWireViewer, shows list of devices */
   private DevicePanel devicePanel;

   /** DSPortAdapter to use */
   private DSPortAdapter adapter;

   /** 1-Wire Network Monitor */
   private NetworkDeviceMonitor monitor;

   /** for storing TaggedDevices */
   private Hashtable tagStore = new Hashtable();

   /** for storing OWFile entries which point to Tags */
   private Hashtable tagFileStore = new Hashtable();

   /** for logging messages */
   private MessageLog log;

   /** for synchronization */
   private Object syncObj = new Object();

   public TagMonitor(MessageLog log)
   {
      this.log = log;
   }

   public void resetSearch()
   {
      monitor.resetSearch();
   }

   public boolean pauseMonitor(boolean blocking)
   {
      return monitor.pauseMonitor(blocking);
   }

   public boolean resumeMonitor(boolean blocking)
   {
      return monitor.resumeMonitor(blocking);
   }

   public void killMonitor()
   {
      monitor.killMonitor();
   }

   public boolean isMonitorRunning()
   {
      return monitor.isMonitorRunning();
   }

   public void setAdapter(DSPortAdapter adapter)
   {
      this.adapter = adapter;
      if(monitor==null)
      {
         monitor = new NetworkDeviceMonitor(adapter);
         monitor.addDeviceMonitorEventListener(this);
         monitor.setBranchAutoSearching(false);
         monitor.pauseMonitor(true);
         Thread t = new Thread(monitor);
         t.start();
      }
      else
      {
         monitor.setAdapter(adapter);
      }
   }

   public void setDevicePanel(DevicePanel devicePanel)
   {
      synchronized(syncObj)
      {
         this.devicePanel = devicePanel;
      }
   }

   public void networkException(DeviceMonitorException e)
   {
      log.addError("TagMonitor", null, e.toString());
   }

   public void deviceArrival(DeviceMonitorEvent dme)
   {
      for(int i = 0; i<dme.getDeviceCount(); i++)
      {
         OneWireContainer owc = dme.getContainerAt(i);
         DSPortAdapter adapter = owc.getAdapter();
         if(!tagFileStore.containsKey(owc))
         {
            log.addVerbose("TagMonitor", null,
                            "Checking device: " + owc.getAddressAsString());
            try
            {
               adapter.beginExclusive(true);
               OWFile tagFile = new OWFile(owc, "TAGX.000");
               if(tagFile.exists())
               {
                  tagFileStore.put(owc, tagFile);
                  log.addVerbose("TagMonitor", null, "Found TAGX.000 file");
                  loadTagsFromStream(adapter, new OWFileInputStream(tagFile));
               }
            }
            catch(Exception e)
            {
               log.addError("TagMonitor", null,
                  "Error while looking for TAGX.00 file: " + e.getMessage());
            }
            finally
            {
               adapter.endExclusive();
            }
         }
      }
      updateDeviceList();
   }

   public void loadTagsFromStream(DSPortAdapter adapter, java.io.InputStream is)
   {
      DefaultListModel lm = this.devicePanel.getListModel();
      try
      {
         TAGParser tagParser = new TAGParser(adapter);
         Enumeration deviceEnum = tagParser.parse(is).elements();
         while(deviceEnum.hasMoreElements())
         {
            TaggedDevice td = (TaggedDevice)deviceEnum.nextElement();
            if(!td.getOWPath().getAllOWPathElements().hasMoreElements())
               td.setOWPath(
                  this.monitor.getDevicePath(
                     td.getDeviceContainer().getAddress()));
            tagStore.put(td.getDeviceContainer(), td);
            lm.addElement(td);
            log.addVerbose("TagMonitor", null, "Got TaggedDevice: " + td);
         }
         devicePanel.updateDeviceLabel();
         Vector paths = tagParser.getOWPaths();
         if(paths.size()>0)
         {
            for(int j=0; j<paths.size(); j++)
            {
               log.addVerbose("TagMonitor", null, "Adding branch to search: "
                                  + paths.elementAt(j));
               monitor.addBranch((OWPath)paths.elementAt(j));
            }
         }
      }
      catch(Exception e)
      {
         log.addError("TagMonitor", null,
            "Error while parsing TAGX.00 file: " + e.getMessage());
      }
   }

   private void updateDeviceList()
   {
      synchronized(syncObj)
      {
      }
   }

   public void deviceDeparture(DeviceMonitorEvent dme)
   {
   }
}