/*---------------------------------------------------------------------------
 * Copyright (C) 2000 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.util.Vector;
import java.util.Enumeration;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.OneWireAccessProvider;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.container.OneWireContainer;
import com.dalsemi.onewire.application.file.OWFileDescriptor;
import com.dalsemi.onewire.application.file.OWFileInputStream;
import com.dalsemi.onewire.application.file.OWFileOutputStream;
import com.dalsemi.onewire.application.file.OWFile;
import com.dalsemi.onewire.application.file.OWSyncFailedException;
import com.dalsemi.onewire.container.MemoryBank;
import com.dalsemi.onewire.container.PagedMemoryBank;

/**
 * Console application to demonstrate file IO on 1-Wire devices.
 *
 * @version    0.01, 1 June 2001
 * @author     DS
 */
public class OWFish
{
   /**
    * Main for 1-Wire File Shell (OWFish)
    */
   public static void main(String args[])
   {
      Vector owd_vect = new Vector(5);
      OneWireContainer[] owd = null;
      DSPortAdapter adapter = null;
      String[] string_list;
      int i, selection, len;
      long start_time=0, end_time;
      FileOutputStream fos;
      FileInputStream fis;
      FileDescriptor fd;
      OWFileOutputStream owfos;
      OWFileInputStream owfis;
      OWFileDescriptor owfd;
      OWFile owfile, new_owfile;
      OWFile[] owfile_list;
      byte[] block = new byte[32];

      System.out.println();
      System.out.println("1-Wire File Shell (OWFish): Version 0.00");
      // os info
      System.out.print("Arch: " + System.getProperty("os.arch"));
      System.out.print(",  OS Name: " + System.getProperty("os.name"));
      System.out.println(",  OS Version: " + System.getProperty("os.version"));
      System.out.println();

      try
      {
         // get the default adapter
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // adapter driver info
         System.out.println("=========================================================================");
         System.out.println("== Adapter Name: " + adapter.getAdapterName());
         System.out.println("== Adapter Port description: " + adapter.getPortTypeDescription());
         System.out.println("== Adapter Version: " + adapter.getAdapterVersion());
         System.out.println("== Adapter support overdrive: " + adapter.canOverdrive());
         System.out.println("== Adapter support hyperdrive: " + adapter.canHyperdrive());
         System.out.println("== Adapter support EPROM programming: " + adapter.canProgram());
         System.out.println("== Adapter support power: " + adapter.canDeliverPower());
         System.out.println("== Adapter support smart power: " + adapter.canDeliverSmartPower());
         System.out.println("== Adapter Class Version: " + adapter.getClassVersion());

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // loop to do menu
         selection = MAIN_SELECT_DEVICE;
         do
         {
            try
            {
               start_time = 0;
               switch (selection)
               {
                  case MAIN_SELECT_DEVICE:
                     // find all parts
                     owd_vect = findAllDevices(adapter);
                     // select a device
                     owd = selectDevice(owd_vect);
                     // check for quite
                     if (owd == null)
                        selection = MAIN_QUIT;
                     else
                     {
                        // display device info
                        System.out.println();
                        System.out.println("  Device(s) selected: ");
                        printDeviceInfo(owd,false);
                     }
                     break;
                  case MAIN_FORMAT:
                     if (menuSelect(verifyMenu) == VERIFY_YES)
                     {
                        // start time of operation
                        start_time = System.currentTimeMillis();
                        // create a 1-Wire file at root
                        owfile = new OWFile(owd, "");
                        // format Filesystem
                        owfile.format();
                        // get 1-Wire File descriptor to flush to device
                        owfd = owfile.getFD();
                        syncFileDescriptor(owfd);
                        // close the 1-Wire file to release
                        owfile.close();
                     }
                     break;
                  case MAIN_LIST:
                     System.out.print("Enter the directory to list on (/ for root): ");
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, getString(1));
                     System.out.println();
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // list the files without recursion
                     listDir(owfile, 1, false);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
                  case MAIN_RLIST:
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, "");
                     System.out.println();
                     // recursive list
                     listDir(owfile, 1, true);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
                  case MAIN_MKDIR:
                     System.out.print("Enter the directory to create (from root): ");
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, getString(1));
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // make the directories
                     if (owfile.mkdirs())
                        System.out.println("Success!");
                     else
                     {
                        System.out.println("-----------------------------------------------");
                        System.out.println("Could not create directories, out of memory or invalid directory/file");
                        System.out.println("-----------------------------------------------");
                     }
                     // get 1-Wire File descriptor to flush to device
                     owfd = owfile.getFD();
                     syncFileDescriptor(owfd);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
                  case MAIN_COPYTO:
                     // system SOURCE file
                     System.out.print("Enter the path/file of the SOURCE file on the system: ");
                     fis = new FileInputStream(getString(1));
                     // 1-Wire DESTINATION file
                     System.out.print("Enter the path/file of the DESTINATION on 1-Wire device: ");
                     owfos = new OWFileOutputStream(owd, getString(1));
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // loop to copy block from SOURCE to DESTINATION
                     do
                     {
                        len = fis.read(block);
                        if (len > 0)
                           owfos.write(block,0,len);
                     }
                     while(len > 0);
                     // get 1-Wire File descriptor to flush to device
                     owfd = owfos.getFD();
                     syncFileDescriptor(owfd);
                     // close the files
                     owfos.close();
                     fis.close();
                     break;
                  case MAIN_COPYFROM:
                     // 1-Wire SOURCE file
                     System.out.print("Enter the path/file of the SOURCE file on 1-Wire device: ");
                     owfis = new OWFileInputStream(owd, getString(1));
                     // system DESTINATION file
                     System.out.print("Enter the path/file of the DESTINATION on system: ");
                     fos = new FileOutputStream(getString(1));
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // loop to copy block from SOURCE to DESTINATION
                     do
                     {
                        len = owfis.read(block);
                        if (len > 0)
                           fos.write(block,0,len);
                     }
                     while(len > 0);
                     // get 1-Wire File descriptor to flush to device
                     fd = fos.getFD();
                     fd.sync();
                     // close the files
                     owfis.close();
                     fos.close();
                     break;
                  case MAIN_CAT:
                     // 1-Wire file
                     System.out.print("Enter the path/file of the file to display: ");
                     owfis = new OWFileInputStream(owd, getString(1));
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     System.out.println();
                     System.out.println("---FILE START---");
                     // loop to read and display file
                     do
                     {
                        len = owfis.read(block);
                        if (len > 0)
                           System.out.print((new String(block,0,len)));
                     }
                     while(len > 0);
                     System.out.println();
                     System.out.println("---FILE END---");
                     // close the file
                     owfis.close();
                     break;
                  case MAIN_DELETE:
                     System.out.print("Enter the directory/file delete: ");
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, getString(1));
                     System.out.println();
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // delete the directory/file
                     if (owfile.delete())
                        System.out.println("Success!");
                     else
                     {
                        System.out.println("-----------------------------------------------");
                        System.out.println("Could not delete, if it is a directory make sure it is empty");
                        System.out.println("-----------------------------------------------");
                     }
                     // get 1-Wire File descriptor to flush to device
                     owfd = owfile.getFD();
                     syncFileDescriptor(owfd);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
                  case MAIN_RENAME:
                     System.out.print("Enter the OLD directory/file name: ");
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, getString(1));
                     System.out.println();
                     System.out.print("Enter the NEW directory/file name: ");
                     // get the directory and create a file on it
                     new_owfile = new OWFile(owd, getString(1));
                     System.out.println();
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // rename the directory/file
                     if (owfile.renameTo(new_owfile))
                        System.out.println("Success!");
                     else
                     {
                        System.out.println("-----------------------------------------------");
                        System.out.println("Could not rename, make sure parents of new directory exist");
                        System.out.println("-----------------------------------------------");
                     }
                     // get 1-Wire File descriptor to flush to device
                     owfd = owfile.getFD();
                     syncFileDescriptor(owfd);
                     // close the 1-Wire file to release
                     owfile.close();
                     new_owfile.close();
                     break;
                  case MAIN_DETAILS:
                     System.out.print("Enter the directory/file to view details: ");
                     // get the directory and create a file on it
                     owfile = new OWFile(owd, getString(1));
                     System.out.println();
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // show the file details
                     showDetails(owfile);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
                  case MAIN_FREEMEM:
                     // start time of operation
                     start_time = System.currentTimeMillis();
                     // create a 1-Wire file at root
                     owfile = new OWFile(owd, "");
                     // get free memory
                     System.out.println();
                     System.out.println("  free memory: " + owfile.getFreeMemory() + " (bytes)");
                     // get the devices participating
                     owd = owfile.getOneWireContainers();
                     System.out.println();
                     System.out.println("  Filesystem consists of: ");
                     printDeviceInfo(owd,true);
                     // close the 1-Wire file to release
                     owfile.close();
                     break;
               };
            }
            catch (IOException e)
            {
               System.out.println();
               System.out.println("-----------------------------------------------");
               System.out.println(e);
               System.out.println("-----------------------------------------------");
            }

            end_time = System.currentTimeMillis();
            System.out.println();
            if (start_time > 0)
               System.out.println((end_time - start_time) + "ms");
            System.out.println();

            if (selection != MAIN_QUIT)
               selection = menuSelect(mainMenu);
            System.out.println();
         }
         while (selection != MAIN_QUIT);

      }
      catch(Exception e)
      {
         System.out.println(e);
      }
      finally
      {
         if (adapter != null)
         {
            // end exclusive use of adapter
            adapter.endExclusive();

            // free the port used by the adapter
            System.out.println("Releasing adapter port");
            try
            {
               adapter.freePort();
            }
            catch (OneWireException e)
            {
               System.out.println(e);
            }
         }
      }

      System.out.println();
      System.exit(0);
   }

   /**
    * List the files/directory entries on the provided 1-Wire File.  Format the
    * output depending on the recursive and depth parameters.
    *
    * @param depth recursive depth
    * @param recursive call this method recursivly if true
    */
   public static void listDir(OWFile owfile, int depth, boolean recursive)
      throws IOException
   {
      OWFile[] owfile_list;

      // get the list
      owfile_list = owfile.listFiles();
      // check for bad directory
      if (owfile_list == null)
         System.out.println("Directory given was not valid or error in Filesystem!");
      else
      {
         // display
         for (int i = 0; i < owfile_list.length; i++)
         {
            if (recursive)
            {
               System.out.print("|");
               if (owfile_list[i].isDirectory())
               {
                  for (int j = 0; j < (depth - 1); j++)
                     System.out.print("    ");

                  if (depth == 1)
                     System.out.print("----");
                  else
                     System.out.print("|---");
               }
               else
               {
                  for (int j = 0; j < depth; j++)
                        System.out.print("    ");
               }
            }

            System.out.print(owfile_list[i].getName());
            if (owfile_list[i].isDirectory())
            {
               if (recursive)
                  System.out.println();
               else
                  System.out.println("    <dir>");
               if (recursive)
                  listDir(owfile_list[i], depth + 1, true);
            }
            else
               System.out.println("  (" + owfile_list[i].length() + " bytes)");

            owfile_list[i].close();
         }
      }
   }

   /**
    * Search for all devices on the provided adapter and return
    * a vector
    *
    * @param  adapter valid 1-Wire adapter
    *
    * @return Vector or OneWireContainers
    */
   public static Vector findAllDevices(DSPortAdapter adapter)
   {
      Vector owd_vect = new Vector(3);
      OneWireContainer owd;

      try
      {
         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

         // enumerate through all the 1-Wire devices and collect them in a vector
         for(Enumeration owd_enum = adapter.getAllDeviceContainers();
             owd_enum.hasMoreElements(); )
         {
            owd = (OneWireContainer)owd_enum.nextElement();
            owd_vect.addElement(owd);

            // set owd to max possible speed with available adapter, allow fall back
            if (adapter.canOverdrive() && (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE))
               owd.setSpeed(owd.getMaxSpeed(),true);
         }
      }
      catch(Exception e)
      {
         System.out.println(e);
      }

      return owd_vect;
   }

   /**
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    *
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer[] selectDevice(Vector owd_vect)
   {
      // create a menu
      String[] menu = new String[owd_vect.size() + 3];
      Vector rewrite = new Vector(1);
      OneWireContainer owd;
      int i;
      OneWireContainer[] oca;
      MemoryBank mb;

      menu[0] = "Device Selection";
      for (i = 0; i < owd_vect.size(); i++)
      {
         owd = (OneWireContainer)owd_vect.elementAt(i);
         menu[i + 1] = new String("(" + i + ") " +
                  owd.getAddressAsString() +
                  " - " + owd.getName());
         if (owd.getAlternateNames().length() > 0)
            menu[i + 1] += "/" + owd.getAlternateNames();

         // collect a list of re-writable devices
         for(Enumeration bank_enum = owd.getMemoryBanks();
                         bank_enum.hasMoreElements(); )
         {
            // get the next memory bank
            mb = (MemoryBank)bank_enum.nextElement();

            // check if desired type
            if (!mb.isWriteOnce() && mb.isGeneralPurposeMemory()
                    && mb.isNonVolatile() && (mb instanceof PagedMemoryBank))
            {
               rewrite.addElement(owd);
               break;
            }
         }
      }
      menu[i + 1] = new String("[" + i + "]--Select All re-writable devices as a multi-device Filesystem");
      menu[i + 2] = new String("[" + (i + 1) + "]--Quit");

      int select = menuSelect(menu);

      // quit
      if (select == (i + 1))
         return null;

      // all re-writable devices
      if (select == i)
      {
         if (rewrite.size() == 0)
            return null;
         oca = new OneWireContainer[rewrite.size()];
         for (i = 0; i < oca.length; i++)
            oca[i] = (OneWireContainer)rewrite.elementAt(i);
      }
      // single device
      else
      {
         oca = new OneWireContainer[1];
         oca[0] = (OneWireContainer)owd_vect.elementAt(select);
      }

      return oca;
   }

   /**
    * Sync's the file Descriptor, prompts for retry if there is
    * an exception.
    *
    * @param fd OWFileDescriptor of Filesystem to sync
    */
   static void syncFileDescriptor(OWFileDescriptor fd)
   {
      for (;;)
      {
         try
         {
            fd.sync();
            return;
         }
         catch (OWSyncFailedException e)
         {
            System.out.println();
            System.out.println("-----------------------------------------------");
            System.out.println(e);
            System.out.println("-----------------------------------------------");

            // prompt to try again
            if (menuSelect(retryMenu) == RETRY_NO)
               return;
         }
      }
   }

   /**
    * Display information about the 1-Wire device
    *
    * @param owd OneWireContainer device
    * @param showMultiType <code> true </code> if want to show designations
    *        in a multi-device Filesystem (MASTER/SATELLITE)
    */
   static void printDeviceInfo(OneWireContainer[] owd, boolean showMultiType)
   {
      for (int dev = 0; dev < owd.length; dev++)
      {
         if (showMultiType && (owd.length > 1))
         {
            if (dev == 0)
               System.out.print("   MASTER:      ");
            else
               System.out.print("   SATELITE(" + dev + "): ");
         }
         else
            System.out.print("   ");

         System.out.println(owd[dev].getName() + ", " + owd[dev].getAddressAsString() +
                            ", (maxspeed) " +
                            ((owd[dev].getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE) ? "Overdrive" : "Normal"));
      }
   }

   /**
    * Display file details
    *
    * @param owfile 1-Wire file to show details of
    */
   static void showDetails(OWFile owfile)
   {
      int[] page_list;
      int local_pg;
      PagedMemoryBank pmb;

      // check if this directory/file exists
      if (!owfile.exists())
      {
         System.out.println("-----------------------------------------------");
         System.out.println("Directory/file not found!");
         System.out.println("-----------------------------------------------");
         return;
      }

      // get the list of pages that make up this directory/file
      System.out.println("Page allocation of " + (owfile.isFile() ? "file:" : "directory:"));

      try
      {
         page_list = owfile.getPageList();
      }
      catch (IOException e)
      {
         System.out.println(e);
         return;
      }

      // loop to display info and contents of each page
      for (int pg = 0; pg < page_list.length; pg++)
      {
         local_pg = owfile.getLocalPage(page_list[pg]);
         System.out.println("Filesystem page=" + page_list[pg] + ", local PagedMemoryBank page=" +
                             owfile.getLocalPage(page_list[pg]));
         pmb = owfile.getMemoryBankForPage(page_list[pg]);
         byte[] read_buf = new byte[pmb.getPageLength()];
         try
         {
            pmb.readPage(local_pg, false, read_buf, 0);
            hexPrint(read_buf,0,read_buf.length);
            System.out.println();
         }
         catch (OneWireException e)
         {
            System.out.println(e);
         }
      }
   }

   /**
    * Display menu and ask for a selection.
    *
    * @param menu Array of strings that represents the menu.
    *        The first element is a title so skip it.
    *
    * @return numberic value entered from the console.
    */
   static int menuSelect(String[] menu)
   {
      System.out.println();
      for (int i = 0; i < menu.length; i++)
         System.out.println(menu[i]);

      System.out.print("Please enter value: ");

      return getNumber(0, menu.length - 2);
   }

   /**
    * Retrieve user input from the console.
    *
    * @param min minimum number to accept
    * @param max maximum number to accept
    *
    * @return numberic value entered from the console.
    */
   static int getNumber (int min, int max)
   {
      int     value   = -1;
      boolean fNumber = false;

      while (fNumber == false)
      {
         try
         {
            String str = getString(1);

            value   = Integer.parseInt(str);

            if ((value < min) || (value > max))
            {
               System.out.println("Invalid value, range must be " + min + " to " + max);
               System.out.print("Please enter value again: ");
            }
            else
               fNumber = true;
         }
         catch (NumberFormatException e)
         {
            System.out.println("Invalid Numeric Value: " + e.toString());
            System.out.print("Please enter value again: ");
         }
      }

      return value;
   }

   private static BufferedReader dis =
            new BufferedReader(new InputStreamReader(System.in));

   /**
    * Retrieve user input from the console.
    *
    * @param minLength minumum length of string
    *
    * @return string entered from the console.
    */
   static String getString (int minLength)
   {
      String str;
      boolean done = false;

      try
      {
         do
         {
            str = dis.readLine();
            if(str.length() < minLength)
               System.out.print("String too short try again:");
            else
               done = true;
         }
         while(!done);

         return str;
      }
      catch (java.io.IOException e)
      {
         System.out.println("Error in reading from console: " + e);
      }

      return "";
   }

   /**
    * Print an array of bytes in hex to standard out.
    *
    * @param  dataBuf data to print
    * @param  offset  offset into dataBuf to start
    * @param  len     length of data to print
    */
   public static void hexPrint(byte[] dataBuf, int offset, int len)
   {
      for (int i = 0; i < len; i++)
      {
         if ((dataBuf[i + offset] & 0x000000FF) < 0x00000010)
         {
            System.out.print("0");
            System.out.print(Integer.toHexString((int)dataBuf[i + offset] & 0x0000000F).toUpperCase());
         }
         else
            System.out.print(Integer.toHexString((int)dataBuf[i + offset] & 0x000000FF).toUpperCase());
      }
   }

   //--------
   //-------- Menus
   //--------

   static final String[] mainMenu = { "----  1-Wire File Shell ----",
                                      "(0) Select Device",
                                      "(1) Directory list",
                                      "(2) Directory list (recursive)",
                                      "(3) Make Directory",
                                      "(4) Copy file TO 1-Wire Filesystem",
                                      "(5) Copy file FROM 1-Wire Filesystem",
                                      "(6) Display contents of file",
                                      "(7) Delete 1-Wire directory/file",
                                      "(8) Rename 1-Wire directory/file",
                                      "(9) Memory available, and Filesystem info",
                                      "(10) Show File details",
                                      "(11) Format Filesystem on 1-Wire device",
                                      "[12]-Quit" };

   static final int MAIN_SELECT_DEVICE = 0;
   static final int MAIN_LIST          = 1;
   static final int MAIN_RLIST         = 2;
   static final int MAIN_MKDIR         = 3;
   static final int MAIN_COPYTO        = 4;
   static final int MAIN_COPYFROM      = 5;
   static final int MAIN_CAT           = 6;
   static final int MAIN_DELETE        = 7;
   static final int MAIN_RENAME        = 8;
   static final int MAIN_FREEMEM       = 9;
   static final int MAIN_DETAILS       = 10;
   static final int MAIN_FORMAT        = 11;
   static final int MAIN_QUIT          = 12;

   static final String[] verifyMenu = { "Format Filesystem on 1-Wire device(s)?",
                                      "(0) NO",
                                      "(1) YES (delete all files/directories)" };

   static final int VERIFY_NO          = 0;
   static final int VERIFY_YES         = 1;

   static final String[] retryMenu = { "RETRY to SYNC with Filesystem on 1-Wire device(s)?",
                                      "(0) NO",
                                      "(1) YES" };

   static final int RETRY_NO           = 0;
   static final int RETRY_YES          = 1;
}
