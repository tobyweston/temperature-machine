
/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.util.*;
import java.io.*;
import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;
import java.lang.InterruptedException;


/**
 * Console application to utilize the MemoryBank features of the
 * OneWireContainers to write blocks and packets.
 *
 * @version    0.01, 15 December 2000
 * @author     DS
 */
public class OWMemUtil
{

   /**
    * Main for 1-Wire Memory utility
    */
   public static void main (String args [])
   {
      Vector           owd_vect = new Vector(5);
      OneWireContainer owd;
      int              i, len, addr, page;
      boolean          done = false;
      String           tstr;
      DSPortAdapter    adapter = null;
      MemoryBank       bank;
      byte[]           data;
      OneWireContainer new_owd;
      long             address = 0;

      System.out.println();
      System.out.println(
         "1-Wire Memory utility console application: Version 0.01");

      // os info
      System.out.print("Arch: " + System.getProperty("os.arch"));
      System.out.print(",  OS Name: " + System.getProperty("os.name"));
      System.out.println(",  OS Version: "
                         + System.getProperty("os.version"));
      System.out.println();

      try
      {

         // get the default adapter
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // adapter driver info
         System.out.println(
            "=========================================================================");
         System.out.println("== Adapter Name: " + adapter.getAdapterName());
         System.out.println("== Adapter Port description: "
                            + adapter.getPortTypeDescription());
         System.out.println("== Adapter Version: "
                            + adapter.getAdapterVersion());
         System.out.println("== Adapter support overdrive: "
                            + adapter.canOverdrive());
         System.out.println("== Adapter support hyperdrive: "
                            + adapter.canHyperdrive());
         System.out.println("== Adapter support EPROM programming: "
                            + adapter.canProgram());
         System.out.println("== Adapter support power: "
                            + adapter.canDeliverPower());
         System.out.println("== Adapter support smart power: "
                            + adapter.canDeliverSmartPower());
         System.out.println("== Adapter Class Version: "
                            + adapter.getClassVersion());

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         // loop to do menu
         do
         {
            // Main menu
            switch (menuSelect(mainMenu))
            {

               case MAIN_SELECT_DEVICE :

                  // find all parts
                  owd_vect = findAllDevices(adapter);

                  // select a device
                  owd = selectDevice(owd_vect);

                  // display device info
                  printDeviceInfo(owd);

                  // select a bank
                  bank = selectBank(owd);

                  // display bank information
                  displayBankInformation(bank);

                  // loop on bank menu
                  do
                  {
                     if(owd instanceof PasswordContainer)
                     {
                        switch (menuSelect(bankPswdMenu))
                        {
                           case BANK_INFO :
                              // display bank information
                              displayBankInformation(bank);
                              break;

                           case BANK_READ_BLOCK :
                              // read a block
                              System.out.print(
                                 "Enter the address to start reading: ");

                              addr = getNumber(0, bank.getSize() - 1);

                              System.out.print(
                                 "Enter the length of data to read: ");

                              len = getNumber(0, bank.getSize());

                              System.out.println();
                              dumpBankBlock(bank, addr, len);
                              break;

                           case BANK_READ_PAGE :
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to read: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);

                                 System.out.println();
                                 dumpBankPage(( PagedMemoryBank ) bank, page);
                              }
                              break;

                           case BANK_READ_UDP :
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to read: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);

                                 System.out.println();
                                 dumpBankPagePacket(( PagedMemoryBank ) bank,
                                                    page);
                              }
                              break;

                           case BANK_WRITE_BLOCK :
                              // write a block
                              System.out.print(
                                 "Enter the address to start writing: ");

                              addr = getNumber(0, bank.getSize() - 1);
                              data = getData();

                              bankWriteBlock(bank, data, addr);
                              break;

                           case BANK_WRITE_UDP :
                              // write a packet
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to write a UDP to: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);
                                 data = getData();

                                 bankWritePacket(( PagedMemoryBank ) bank, data,
                                                 page);
                              }
                              break;

                           case BANK_NEW_PASS_BANK :
                              // select a bank
                              bank = selectBank(owd);
                              break;

                           case BANK_NEW_MAIN_MENU :
                              done = true;
                              break;

                           case BANK_READONLY_PASS :
                              System.out.println(
                                 "Enter data for the read only password.");
                              data = getData();

                              ((PasswordContainer)owd).setDeviceReadOnlyPassword(data, 0);
                              break;

                           case BANK_READWRIT_PASS :
                              System.out.println(
                                 "Enter data for the read/write password.");
                              data = getData();

                              ((PasswordContainer)owd).setDeviceReadWritePassword(data, 0);
                              break;

                           case BANK_WRITEONLY_PASS :
                              System.out.println(
                                 "Enter data for the write only password.");
                              data = getData();

                              ((PasswordContainer)owd).setDeviceWriteOnlyPassword(data, 0);
                              break;

                           case BANK_BUS_RO_PASS :
                              System.out.println(
                                 "Enter data for the read only password.");
                              data = getData();

                              ((PasswordContainer)owd).setContainerReadOnlyPassword(data, 0);
                              break;

                           case BANK_BUS_RW_PASS :
                              System.out.println(
                                 "Enter data for the read/write password.");
                              data = getData();

                              ((PasswordContainer)owd).setContainerReadWritePassword(data, 0);
                              break;

                           case BANK_BUS_WO_PASS :
                              System.out.println(
                                 "Enter data for the write only password.");
                              data = getData();

                              ((PasswordContainer)owd).setContainerWriteOnlyPassword(data, 0);
                              break;

                           case BANK_ENABLE_PASS :
                              ((PasswordContainer)owd).setDevicePasswordEnableAll(true);
                              break;

                           case BANK_DISABLE_PASS :
                              ((PasswordContainer)owd).setDevicePasswordEnableAll(false);
                              break;
                        }
                     }
                     else
                     {
                        switch (menuSelect(bankMenu))
                        {

                           case BANK_INFO :

                              // display bank information
                              displayBankInformation(bank);
                              break;
                           case BANK_READ_BLOCK :

                              // read a block
                              System.out.print(
                                 "Enter the address to start reading: ");

                              addr = getNumber(0, bank.getSize() - 1);

                              System.out.print(
                                 "Enter the length of data to read: ");

                              len = getNumber(0, bank.getSize());

                              System.out.println();
                              dumpBankBlock(bank, addr, len);
                              break;
                           case BANK_READ_PAGE :
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to read: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);

                                 System.out.println();
                                 dumpBankPage(( PagedMemoryBank ) bank, page);
                              }
                              break;
                           case BANK_READ_UDP :
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to read: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);

                                 System.out.println();
                                 dumpBankPagePacket(( PagedMemoryBank ) bank,
                                                    page);
                              }
                              break;
                           case BANK_WRITE_BLOCK :

                              // write a block
                              System.out.print(
                                 "Enter the address to start writing: ");

                              addr = getNumber(0, bank.getSize() - 1);
                              data = getData();

                              bankWriteBlock(bank, data, addr);
                              break;
                           case BANK_WRITE_UDP :

                              // write a packet
                              if (!(bank instanceof PagedMemoryBank))
                                 System.out.print(
                                    "Bank is not a 'PagedMemoryBank'");
                              else
                              {
                                 System.out.print(
                                    "Enter the page number to write a UDP to: ");

                                 page = getNumber(
                                    0, (( PagedMemoryBank ) bank).getNumberPages()
                                    - 1);
                                 data = getData();

                                 bankWritePacket(( PagedMemoryBank ) bank, data,
                                                 page);
                              }
                              break;
                           case BANK_NEW_BANK :

                              // select a bank
                              bank = selectBank(owd);
                              break;
                           case BANK_MAIN_MENU :
                              done = true;
                              break;
                        }
                     }
                  }
                  while (!done);

                  done = false;
                  break;
               case MAIN_TEST :

                  // find all parts
                  owd_vect = findAllDevices(adapter);

                  // test menu
                  switch (menuSelect(testMenu))
                  {

                     case TEST_WRITE_BLOCK :
                        System.out.print(
                           "Enter the character to write to the entire device: ");

                        tstr = getString(1);

                        for (i = 0; i < owd_vect.size(); i++)
                           writeTestBlocks(
                              ( OneWireContainer ) owd_vect.elementAt(i),
                              ( byte ) tstr.charAt(0));
                        break;
                     case TEST_WRITE_PKTS :
                        System.out.print(
                           "Enter the length of data in the packets (0-29): ");

                        len = getNumber(0, 29);

                        System.out.print(
                           "Enter the character to write to the entire device: ");

                        tstr = getString(1);

                        for (i = 0; i < owd_vect.size(); i++)
                           writeTestPkts(
                              ( OneWireContainer ) owd_vect.elementAt(i),
                              ( byte ) tstr.charAt(0), len);
                        break;
                     case TEST_READ_RAW :
                        for (i = 0; i < owd_vect.size(); i++)
                           dumpDeviceRaw(
                              ( OneWireContainer ) owd_vect.elementAt(i));
                        break;
                     case TEST_READ_PKTS :
                        for (i = 0; i < owd_vect.size(); i++)
                           dumpDevicePackets(
                              ( OneWireContainer ) owd_vect.elementAt(i));
                        break;
                     case TEST_QUIT :
                        done = true;
                        break;
                  }
                  break;
               case MAIN_QUIT :
                  done = true;
                  break;
            }
         }
         while (!done);
      }
      catch (Exception e)
      {
         e.printStackTrace();
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
    * Search for all devices on the provided adapter and return
    * a vector
    *
    * @param  adapter valid 1-Wire adapter
    *
    * @return Vector or OneWireContainers
    */
   public static Vector findAllDevices (DSPortAdapter adapter)
   {
      Vector           owd_vect = new Vector(3);
      OneWireContainer owd;

      try
      {

         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

         // enumerate through all the 1-Wire devices and collect them in a vector
         for (Enumeration owd_enum = adapter.getAllDeviceContainers();
                 owd_enum.hasMoreElements(); )
         {
            owd = ( OneWireContainer ) owd_enum.nextElement();

            owd_vect.addElement(owd);

            // set owd to max possible speed with available adapter, allow fall back
            if (adapter.canOverdrive()
                    && (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE))
               owd.setSpeed(owd.getMaxSpeed(), true);
         }

      }
      catch (Exception e)
      {
         System.out.println(e);
      }

      return owd_vect;
   }

   //--------
   //-------- Read methods
   //--------

   /**
    * Read a block from a memory bank and print in hex
    *
    * @param  bank  MemoryBank to read a block from
    * @param  addr  address to start reading from
    * @param  len   length of data to read
    */
   public static void dumpBankBlock (MemoryBank bank, int addr, int len)
   {
      try
      {
         byte[] read_buf = new byte [len];

         // read the entire bank
         bank.read(addr, false, read_buf, 0, len);
         hexPrint(read_buf, 0, len);
         System.out.println("");
      }
      catch (Exception e)
      {
         System.out.println(e);
      }
   }

   /**
    * Read a page from a memory bank and print in hex
    *
    * @param  bank  PagedMemoryBank to read a page from
    * @param  pg  page to read
    */
   public static void dumpBankPage (PagedMemoryBank bank, int pg)
   {
      byte[] read_buf  = new byte [bank.getPageLength()];
      byte[] extra_buf = new byte [bank.getExtraInfoLength()];

      try
      {

         // read a page (use the most verbose and secure method)
         if (bank.hasPageAutoCRC())
         {
            System.out.println("Using device generated CRC");

            if (bank.hasExtraInfo())
               bank.readPageCRC(pg, false, read_buf, 0, extra_buf);
            else
               bank.readPageCRC(pg, false, read_buf, 0);
         }
         else
         {
            if (bank.hasExtraInfo())
               bank.readPage(pg, false, read_buf, 0, extra_buf);
            else
               bank.readPage(pg, false, read_buf, 0);
         }

         System.out.print("Page " + pg + ": ");
         hexPrint(read_buf, 0, read_buf.length);
         System.out.println("");

         if (bank.hasExtraInfo())
         {
            System.out.print("Extra: ");
            hexPrint(extra_buf, 0, bank.getExtraInfoLength());
            System.out.println("");
         }
      }
      catch (Exception e)
      {
         System.out.println(e);
      }
   }

   /**
    * Read a page packet from a memory bank and print in hex
    *
    * @param  bank  PagedMemoryBank to read a page from
    * @param  pg  page to read
    */
   public static void dumpBankPagePacket (PagedMemoryBank bank, int pg)
   {
      byte[] read_buf  = new byte [bank.getPageLength()];
      byte[] extra_buf = new byte [bank.getExtraInfoLength()];
      int    read_rslt;

      try
      {

         // read a page packet (use the most verbose method)
         if (bank.hasExtraInfo())
            read_rslt = bank.readPagePacket(pg, false, read_buf, 0,
                                            extra_buf);
         else
            read_rslt = bank.readPagePacket(pg, false, read_buf, 0);

         System.out.print("Packet " + pg + ", len " + read_rslt + ": ");
         hexPrint(read_buf, 0, read_rslt);
         System.out.println("");

         if (bank.hasExtraInfo())
         {
            System.out.print("Extra: ");
            hexPrint(extra_buf, 0, bank.getExtraInfoLength());
            System.out.println("");
         }
      }
      catch (Exception e)
      {
         System.out.println(e);
      }
   }

   /**
    * Dump all valid memory packets from all general-purpose memory banks.
    * in the provided OneWireContainer instance.
    *
    * @parameter owd device to check for memory banks.
    */
   public static void dumpDevicePackets (OneWireContainer owd)
   {
      byte[]  read_buf, extra_buf;
      long    start_time, end_time;
      int     read_rslt;
      boolean found_bank = false;

      // display device info
      printDeviceInfo(owd);

      // set to max possible speed
      owd.setSpeed(owd.getMaxSpeed(), true);

      // get the port names we can use and try to open, test and close each
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {

         // get the next memory bank
         MemoryBank mb = ( MemoryBank ) bank_enum.nextElement();

         // check if desired type, only look for packets in general non-volatile
         if (!mb.isGeneralPurposeMemory() ||!mb.isNonVolatile())
            continue;

         // check if has paged services
         if (!(mb instanceof PagedMemoryBank))
            continue;

         // found a memory bank
         found_bank = true;

         // cast to page bank
         PagedMemoryBank bank = ( PagedMemoryBank ) mb;

         // display bank information
         displayBankInformation(bank);

         read_buf  = new byte [bank.getPageLength()];
         extra_buf = new byte [bank.getExtraInfoLength()];

         // start timer to time the dump of the bank contents
         start_time = System.currentTimeMillis();

         // loop to read all of the pages in bank
         boolean readContinue = false;

         for (int pg = 0; pg < bank.getNumberPages(); pg++)
         {
            try
            {

               // read a page packet (use the most verbose and secure method)
               if (bank.hasExtraInfo())
                  read_rslt = bank.readPagePacket(pg, readContinue, read_buf,
                                                  0, extra_buf);
               else
                  read_rslt = bank.readPagePacket(pg, readContinue, read_buf,
                                                  0);

               if (read_rslt >= 0)
               {
                  readContinue = true;

                  System.out.print("Packet " + pg + " (" + read_rslt + "): ");
                  hexPrint(read_buf, 0, read_rslt);
                  System.out.println("");

                  if (bank.hasExtraInfo())
                  {
                     System.out.print("Extra: ");
                     hexPrint(extra_buf, 0, bank.getExtraInfoLength());
                     System.out.println("");
                  }
               }
               else
               {
                  System.out.println("Error reading page : " + pg);

                  readContinue = false;
               }
            }
            catch (Exception e)
            {
               System.out.println("Exception in reading page: " + e
                                  + "TRACE: ");

               readContinue = false;
            }
         }

         end_time = System.currentTimeMillis();

         System.out.println("     (time to read PACKETS = "
                            + Long.toString(end_time - start_time) + "ms)");
      }

      if (!found_bank)
         System.out.println(
            "XXXX Does not contain any general-purpose non-volatile page memory bank's");
   }

   /**
    * Dump all of the 1-Wire readable memory in the provided
    * Memory Banks of the OneWireContainer instance.
    *
    * @parameter owd device to check for memory banks.
    */
   public static void dumpDeviceRaw (OneWireContainer owd)
   {
      boolean found_bank = false;

      // display device info
      printDeviceInfo(owd);

      // set to max possible speed
      owd.setSpeed(owd.getMaxSpeed(), true);

      // loop through all of the memory banks on device
      // get the port names we can use and try to open, test and close each
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {

         // get the next memory bank
         MemoryBank bank = ( MemoryBank ) bank_enum.nextElement();

         // display bank information
         displayBankInformation(bank);

         // found a memory bank
         found_bank = true;

         // dump the bank
         dumpBankBlock(bank, 0, bank.getSize());
      }

      if (!found_bank)
         System.out.println("XXXX Does not contain any memory bank's");
   }

   //--------
   //-------- Write methods
   //--------

   /**
    * Write a block of data with the provided MemoryBank.
    *
    * @param  bank  MemoryBank to write block to
    * @param  data  data to write in a byte array
    * @param  addr  address to start the write
    */
   public static void bankWriteBlock (MemoryBank bank, byte[] data, int addr)
   {
      try
      {
         bank.write(addr, data, 0, data.length);
         System.out.println();
         System.out.println("wrote block length " + data.length + " at addr "
                            + addr);
      }
      catch (Exception e)
      {
         System.out.println(e);
      }
   }

   /**
    * Write a UDP packet to the specified page in the
    * provided PagedMemoryBank.
    *
    * @param  bank  PagedMemoryBank to write packet to
    * @param  data  data to write in a byte array
    * @param  pg    page number to write packet to
    */
   public static void bankWritePacket (PagedMemoryBank bank, byte[] data,
                                       int pg)
   {
      try
      {
         bank.writePagePacket(pg, data, 0, data.length);
         System.out.println();
         System.out.println("wrote packet length " + data.length
                            + " on page " + pg);
      }
      catch (Exception e)
      {
         System.out.println(e);
      }
   }

   //--------
   //-------- Menu methods
   //--------

   /**
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    *
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer selectDevice (Vector owd_vect)
      throws InterruptedException
   {

      // create a menu
      String[]         menu = new String [owd_vect.size() + 2];
      OneWireContainer owd;
      int              i;

      menu [0] = "Device Selection";

      for (i = 0; i < owd_vect.size(); i++)
      {
         owd          = ( OneWireContainer ) owd_vect.elementAt(i);
         menu [i + 1] = new String("(" + i + ") " + owd.getAddressAsString()
                                   + " - " + owd.getName());

         if (owd.getAlternateNames().length() > 0)
            menu [i + 1] += "/" + owd.getAlternateNames();
      }

      menu [i + 1] = new String("[" + i + "]--Quit");

      int select = menuSelect(menu);

      if (select == i)
         throw new InterruptedException("Quit in device selection");

      return ( OneWireContainer ) owd_vect.elementAt(select);
   }

   /**
    * Create a menu of memory banks from the provided OneWireContainer
    * allow the user to select one.
    *
    * @param  owd devices to choose a MemoryBank from
    *
    * @return MemoryBank memory bank selected
    */
   public static MemoryBank selectBank (OneWireContainer owd)
      throws InterruptedException
   {

      // create a menu
      Vector     banks = new Vector(3);
      int        i;
      MemoryBank mb;

      // get a vector of the banks
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {
         banks.addElement(( MemoryBank ) bank_enum.nextElement());
      }

      String[] menu = new String [banks.size() + 2];

      menu [0] = "Memory Bank Selection for " + owd.getAddressAsString()
                 + " - " + owd.getName();

      if (owd.getAlternateNames().length() > 0)
         menu [0] += "/" + owd.getAlternateNames();

      for (i = 0; i < banks.size(); i++)
      {
         menu [i + 1] = new String(
            "(" + i + ") "
            + (( MemoryBank ) banks.elementAt(i)).getBankDescription());
      }
      menu [i + 1]    = new String("[" + i + "]--Quit");

      int select = menuSelect(menu);

      if (select == i)
         throw new InterruptedException("Quit in bank selection");

      return ( MemoryBank ) banks.elementAt(select);
   }

   //--------
   //-------- Menu Methods
   //--------

   /**
    * Display menu and ask for a selection.
    *
    * @param menu Array of strings that represents the menu.
    *        The first element is a title so skip it.
    *
    * @return numberic value entered from the console.
    */
   static int menuSelect (String[] menu)
   {
      System.out.println();

      for (int i = 0; i < menu.length; i++)
         System.out.println(menu [i]);

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

            value = Integer.parseInt(str);

            if ((value < min) || (value > max))
            {
               System.out.println("Invalid value, range must be " + min
                                  + " to " + max);
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
    * Retrieve user input from the console in the form of hex or text.
    *
    * @return byte array of data.
    */
   public static byte[] getData ()
   {
      byte[]  data     = null;
      boolean got_data = false;

      if (menuSelect(modeMenu) == MODE_TEXT)
      {
         String tstr = getString(1);

         data = tstr.getBytes();
      }
      else
      {
         do
         {
            try
            {
               String tstr = getString(2);

               data     = parseByteString(tstr);
               got_data = true;
            }
            catch (StringIndexOutOfBoundsException e)
            {
               System.out.println(e);
               System.out.println("Enter Hex data again");
            }
         }
         while (!got_data);
      }

      System.out.print("Data to write, len (" + data.length + ") :");
      hexPrint(data, 0, data.length);
      System.out.println();

      return data;
   }

   //--------
   //-------- Test Methods
   //--------

   /**
    * Write a block to every memory bank in this device that is general-purpose
    * and non-volatile
    *
    * @param  owd          device to write block to
    * @param  data         data byte to write
    */
   public static void writeTestBlocks (OneWireContainer owd, byte data)
   {
      long start_time, end_time;

      // display device info
      printDeviceInfo(owd);

      // set to max possible speed
      owd.setSpeed(owd.getMaxSpeed(), false);

      // get the port names we can use and try to open, test and close each
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {

         // get the next memory bank
         MemoryBank bank = ( MemoryBank ) bank_enum.nextElement();

         // display bank information
         displayBankInformation(bank);

         // check if desired type
         if (!bank.isGeneralPurposeMemory() ||!bank.isNonVolatile())
         {
            System.out.println(
               "**** Not general-purpose/non-volatile so skipping this bank ****");

            continue;
         }

         // write the block
         try
         {
            byte[] wr_buf = new byte [bank.getSize()];

            for (int i = 0; i < wr_buf.length; i++)
               wr_buf [i] = ( byte ) data;

            // start timer to time the dump of the bank contents
            start_time = System.currentTimeMillis();

            bank.write(0, wr_buf, 0, wr_buf.length);
            System.out.println("wrote block (" + wr_buf.length
                               + ") at addr 0");

            end_time = System.currentTimeMillis();

            System.out.println("     (time to write = "
                               + Long.toString(end_time - start_time)
                               + "ms)");
         }
         catch (Exception e)
         {
            System.out.println("Exception writing: " + e + "TRACE: ");
            e.printStackTrace();
         }
      }
   }

   /**
    * Write a packet to every page in every memory bank in this device that is general-purpose
    * and non-volatile
    *
    * @param  owd          device to write block to
    * @param  data         data byte to write
    * @param  len          length of data to write
    */
   public static void writeTestPkts (OneWireContainer owd, byte data, int len)
   {
      long start_time, end_time;

      // display device info
      printDeviceInfo(owd);

      // set to max possible speed
      owd.setSpeed(owd.getMaxSpeed(), false);

      // get the port names we can use and try to open, test and close each
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {

         // get the next memory bank
         MemoryBank bank = ( MemoryBank ) bank_enum.nextElement();

         // display bank information
         displayBankInformation(bank);

         // check if desired type
         if (!bank.isGeneralPurposeMemory() ||!bank.isNonVolatile())
         {
            System.out.println(
               "**** Not general-purpose/non-volatile so skipping this bank ****");

            continue;
         }

         // check if has paged services
         if (!(bank instanceof PagedMemoryBank))
         {
            System.out.println(
               "**** Not PagedMemoryBank so skipping this bank ****");

            continue;
         }

         // caste to page bank
         PagedMemoryBank pbank  = ( PagedMemoryBank ) bank;
         byte[]          wr_buf = new byte [pbank.getPageLength()];

         for (int i = 0; i < len; i++)
            wr_buf [i] = ( byte ) data;

         // start timer to time the dump of the bank contents
         start_time = System.currentTimeMillis();

         // loop to read all of the pages in bank
         for (int pg = 0; pg < pbank.getNumberPages(); pg++)
         {
            try
            {
               pbank.writePagePacket(pg, wr_buf, 0, len);
               System.out.println("wrote " + len + " byte packet on page "
                                  + pg);
            }
            catch (Exception e)
            {
               System.out.println("Exception writing: " + e + "TRACE: ");
               e.printStackTrace();

               return;
            }
         }

         end_time = System.currentTimeMillis();

         System.out.println("Time to write: "
                            + Long.toString(end_time - start_time) + "ms");
      }
   }

   //--------
   //-------- Display Methods
   //--------

   /**
    * Display information about the 1-Wire device
    *
    * @param owd OneWireContainer device
    */
   static void printDeviceInfo (OneWireContainer owd)
   {
      System.out.println();
      System.out.println(
         "*************************************************************************");
      System.out.println("* Device Name: " + owd.getName());
      System.out.println("* Device Other Names: " + owd.getAlternateNames());
      System.out.println("* Device Address: " + owd.getAddressAsString());
      System.out.println(
         "* Device Max speed: "
         + ((owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE)
            ? "Overdrive"
            : "Normal"));
      System.out.println("* iButton Description: " + owd.getDescription());
   }

   /**
    * Display the information about the current memory back provided.
    *
    * @param bank Memory Bank object.
    */
   public static void displayBankInformation (MemoryBank bank)
   {
      System.out.println(
         "|------------------------------------------------------------------------");
      System.out.println("| Bank: (" + bank.getBankDescription() + ")");
      System.out.print("| Implements : MemoryBank");

      if (bank instanceof PagedMemoryBank)
         System.out.print(", PagedMemoryBank");

      if (bank instanceof OTPMemoryBank)
         System.out.print(", OTPMemoryBank");

      System.out.println();
      System.out.println("| Size " + bank.getSize()
                         + " starting at physical address "
                         + bank.getStartPhysicalAddress());
      System.out.print("| Features:");

      if (bank.isReadWrite())
         System.out.print(" Read/Write");

      if (bank.isWriteOnce())
         System.out.print(" Write-once");

      if (bank.isReadOnly())
         System.out.print(" Read-only");

      if (bank.isGeneralPurposeMemory())
         System.out.print(" general-purpose");
      else
         System.out.print(" not-general-purpose");

      if (bank.isNonVolatile())
         System.out.print(" non-volatile");
      else
         System.out.print(" volatile");

      if (bank.needsProgramPulse())
         System.out.print(" needs-program-pulse");

      if (bank.needsPowerDelivery())
         System.out.print(" needs-power-delivery");

      // check if has paged services
      if (bank instanceof PagedMemoryBank)
      {

         // caste to page bank
         PagedMemoryBank pbank = ( PagedMemoryBank ) bank;

         // page info
         System.out.println();
         System.out.print("| Pages: " + pbank.getNumberPages()
                          + " pages of length ");
         System.out.print(pbank.getPageLength() + " bytes ");

         if (bank.isGeneralPurposeMemory())
            System.out.print("giving " + pbank.getMaxPacketDataLength()
                             + " bytes Packet data payload");

         if (pbank.hasPageAutoCRC())
         {
            System.out.println();
            System.out.print("| Page Features: page-device-CRC");
         }

         // check if has OTP services
         if (pbank instanceof OTPMemoryBank)
         {

            // caste to OTP bank
            OTPMemoryBank ebank = ( OTPMemoryBank ) pbank;

            if (ebank.canRedirectPage())
               System.out.print(" pages-redirectable");

            if (ebank.canLockPage())
               System.out.print(" pages-lockable");

            if (ebank.canLockRedirectPage())
               System.out.print(" redirection-lockable");
         }

         if (pbank.hasExtraInfo())
         {
            System.out.println();
            System.out.println("| Extra information for each page:  "
                               + pbank.getExtraInfoDescription()
                               + ", length " + pbank.getExtraInfoLength());
         }
         else
            System.out.println();
      }
      else
         System.out.println();

      System.out.println(
         "|------------------------------------------------------------------------");
   }

   /**
    * Print an array of bytes in hex to standard out.
    *
    * @param  dataBuf data to print
    * @param  offset  offset into dataBuf to start
    * @param  len     length of data to print
    */
   public static void hexPrint (byte[] dataBuf, int offset, int len)
   {
      for (int i = 0; i < len; i++)
      {
         if ((dataBuf [i + offset] & 0x000000FF) < 0x00000010)
         {
            System.out.print("0");
            System.out.print(Integer.toHexString(( int ) dataBuf [i + offset]
                                                 & 0x0000000F).toUpperCase());
         }
         else
            System.out.print(Integer.toHexString(( int ) dataBuf [i + offset]
                                                 & 0x000000FF).toUpperCase());
      }
   }

   /**
    * parse byte string into a byte array
    *
    * @param  str  String to parse
    *
    * @return byte array of data.
    */
   static byte[] parseByteString (String str)
   {

      // data are entered in "xx xx xx xx" format
      String dataStr  = str.trim();
      int    dataLen  = dataStr.length();
      byte[] buf      = new byte [dataLen];
      int    bufLen   = 0;
      int    curPos   = 0;
      int    savedPos = 0;
      int    count    = 0;
      char   c;

      while (curPos < dataLen)
      {
         c = dataStr.charAt(curPos);

         if (!Character.isWhitespace(c))
         {
            savedPos = curPos;
            count    = 1;

            while ((curPos < dataLen - 1)
                   && (!Character.isWhitespace(dataStr.charAt(++curPos))))
            {
               count++;
            }

            if (count > 2)
               throw new StringIndexOutOfBoundsException(
                  "Invalid Byte String: " + str);

            if (curPos != dataLen - 1)
               curPos--;

            if (count == 1)   // only 1 digit entered
               buf [bufLen++] = ( byte ) hexDigitValue(c);
            else
               buf [bufLen++] =
                  ( byte ) ((hexDigitValue(c) << 4)
                            | ( byte ) hexDigitValue(dataStr.charAt(curPos)));
         }                    // if

         curPos++;
      }                       // while

      byte[] data = new byte [bufLen];

      System.arraycopy(buf, 0, data, 0, bufLen);

      return data;
   }

   /**
    * convert input to hexidecimal value
    *
    * @param  c  hex char to convert
    *
    * @return int representation of hex character
    */
   static int hexDigitValue (char c)
   {
      int value = Character.digit(c, 16);

      if (value == -1)
      {
         throw new StringIndexOutOfBoundsException("Invalid Hex value: " + c);
      }

      return value;
   }

   //--------
   //-------- Menus
   //--------
   static final String[] mainMenu           = { "MainMenu 1-Wire Memory Demo",
                                                "(0) Select Device",
                                                "(1) Test mode", "(2) Quit" };
   static final int      MAIN_SELECT_DEVICE = 0;
   static final int      MAIN_TEST          = 1;
   static final int      MAIN_QUIT          = 2;
   static final String[] bankMenu           =
   {
      "Bank Operation Menu", "(0) Get Bank information",
      "(1) Read Block", "(2) Read Page",
      "(3) Read Page UDP packet", "(4) Write Block",
      "(5) Write UDP packet", "(6) GOTO MemoryBank Menu", "(7) GOTO MainMenu"
   };
   static final int      BANK_INFO          = 0;
   static final int      BANK_READ_BLOCK    = 1;
   static final int      BANK_READ_PAGE     = 2;
   static final int      BANK_READ_UDP      = 3;
   static final int      BANK_WRITE_BLOCK   = 4;
   static final int      BANK_WRITE_UDP     = 5;
   static final int      BANK_NEW_BANK      = 6;
   static final int      BANK_MAIN_MENU     = 7;
   static final String[] bankPswdMenu =
   {
      "Bank Password Operation Menu", "(0) Get Bank information",
      "(1) Read Block", "(2) Read Page", "(3) Read Page UDP packet",
      "(4) Write Block", "(5) Write UDP packet", "(6) Set Read-Only Password",
      "(7) Set Read/Write password", "(8) Set Write Only Password",
      "(9) Set Container Read-Only Password", "(10) Set Container Read/Write Password",
      "(11) Set Container Write-Only Password", "(12) Enable Device Passwords",
      "(13) Disable Device Passwords", "(14) GOTO MemoryBank Menu", "(15) GOTO MainMenu"
   };
   static final int      BANK_READONLY_PASS   = 6;
   static final int      BANK_READWRIT_PASS   = 7;
   static final int      BANK_WRITEONLY_PASS  = 8;
   static final int      BANK_BUS_RO_PASS     = 9;
   static final int      BANK_BUS_RW_PASS     = 10;
   static final int      BANK_BUS_WO_PASS     = 11;
   static final int      BANK_ENABLE_PASS     = 12;
   static final int      BANK_DISABLE_PASS    = 13;
   static final int      BANK_NEW_PASS_BANK   = 14;
   static final int      BANK_NEW_MAIN_MENU   = 15;

   static final String[] testMenu           =
   {
      "TestMode, for all general-purpose MemoryBanks",
      "(0) Write entire bank with same value",
      "(1) Write UDP packets to all pages",
      "(2) Read all banks", "(3) Read UDP packets on all pages", "(4) Quit"
   };
   static final int      TEST_WRITE_BLOCK   = 0;
   static final int      TEST_WRITE_PKTS    = 1;
   static final int      TEST_READ_RAW      = 2;
   static final int      TEST_READ_PKTS     = 3;
   static final int      TEST_QUIT          = 4;
   static final String[] modeMenu           = { "Data Entry Mode",
                                                "(0) Text (single line)",
                                                "(1) Hex (XX XX XX XX ...)" };
   static final int      MODE_TEXT          = 0;
   static final int      MODE_HEX           = 1;
}
