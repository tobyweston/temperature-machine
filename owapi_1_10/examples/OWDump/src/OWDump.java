
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


/**
 * 1-Wire memory Dump demo.
 *
 *  @version    0.01, 15 December 2000
 *  @author     DS
 */
public class OWDump
{

   /**
    * Main for OWDump
    */
   public static void main (String args [])
   {
      System.out.println();
      System.out.println(
         "OneWire Memory Dump console application: Version 0.01");

      // os info
      System.out.print("Arch: " + System.getProperty("os.arch"));
      System.out.print(",  OS Name: " + System.getProperty("os.name"));
      System.out.println(",  OS Version: "
                         + System.getProperty("os.version"));
      System.out.println();

      // check for correct command line parameters
      boolean argsOK = false;

      if ((args.length >= 1) && (args.length <= 2))
      {
         argsOK = true;

         // check for valid flag
         if ((args [0].indexOf("r") == -1) && (args [0].indexOf("k") == -1)
                 && (args [0].indexOf("p") == -1))
         {
            System.out.println("Unsupported flag: " + args [0]);

            argsOK = false;
         }
      }

      if (!argsOK)
      {
         System.out.println();
         System.out.println("syntax: OWDump ('r' 'p' 'k') <TIME_TEST>");
         System.out.println(
            "   Dump an iButton/1-Wire Device's memory contents");
         System.out.println(
            "   'r' 'p' 'k' - required flag: (Raw,Page,pacKet) type dump");
         System.out.println(
            "   <TIME_TEST> - optional flag if present will time each read ");
         System.out.println(
            "                 of the memory banks and not display the contents");
         System.exit(0);
      }

      try
      {

         // get the default adapter  
         DSPortAdapter adapter = OneWireAccessProvider.getDefaultAdapter();

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

         // clear any previous search restrictions
         adapter.setSearchAllDevices();
         adapter.targetAllFamilies();
         adapter.setSpeed(adapter.SPEED_REGULAR);

         // enumerate through all the iButtons found
         for (Enumeration owd_enum = adapter.getAllDeviceContainers();
                 owd_enum.hasMoreElements(); )
         {

            // get the next owd
            OneWireContainer owd =
               ( OneWireContainer ) owd_enum.nextElement();

            System.out.println();
            System.out.println(
               "*************************************************************************");
            System.out.println("* 1-Wire Device Name: " + owd.getName());
            System.out.println("* 1-Wire Device Other Names: "
                               + owd.getAlternateNames());
            System.out.println("* 1-Wire Device Address: "
                               + owd.getAddressAsString());
            System.out.println(
               "* 1-Wire Device Max speed: "
               + ((owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE)
                  ? "Overdrive"
                  : "Normal"));
            System.out.println("* 1-Wire Device Description: "
                               + owd.getDescription());

            // set owd to max possible speed with available adapter, allow fall back
            if (adapter.canOverdrive()
                    && (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE))
               owd.setSpeed(owd.getMaxSpeed(), true);

            // dump raw contents of all memory banks
            if (args [0].indexOf("r") != -1)
            {
               dumpDeviceRaw(owd, (args.length == 1));
            }

            // dump page packets of non-volatile general-purpose memory banks
            else if (args [0].indexOf("k") != -1)
            {
               dumpDevicePackets(owd, (args.length == 1));
            }

            // dump pages of memory bank 
            else if (args [0].indexOf("p") != -1)
            {
               dumpDevicePages(owd, (args.length == 1));
            }
            else
               System.out.println("No action taken, unsupported flag");
         }

         // end exclusive use of adapter
         adapter.endExclusive();

         // free the port used by the adapter
         System.out.println("Releasing adapter port");
         adapter.freePort();
      }
      catch (Exception e)
      {
         System.out.println("Exception: " + e);
         e.printStackTrace();
      }

      System.out.println();
      System.exit(0);
   }

   /**
    * Dump all of the 1-Wire readable memory in the provided
    * MemoryContainer instance.
    *
    * @parameter owd device to check for memory banks.
    * @parameter showContents flag to indicate if the memory bank contents will
    *                      be displayed
    */
   public static void dumpDeviceRaw (OneWireContainer owd,
                                     boolean showContents)
   {
      byte[]  read_buf, extra_buf;
      long    start_time, end_time;
      boolean found_bank = false;
      int     i, reps = 10;

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

         try
         {
            read_buf = new byte [bank.getSize()];

            // get overdrive going so not a factor in time tests
            bank.read(0, false, read_buf, 0, 1);

            // dynamically change number of reps
            reps = 1500 / read_buf.length;

            if (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE)
               reps *= 2;

            if ((reps == 0) || showContents)
               reps = 1;

            if (!showContents)
               System.out.print("[" + reps + "]");

            // start timer to time the dump of the bank contents         
            start_time = System.currentTimeMillis();

            // read the entire bank
            for (i = 0; i < reps; i++)
               bank.read(0, false, read_buf, 0, bank.getSize());

            end_time = System.currentTimeMillis();

            System.out.println("     (time to read RAW = "
                               + Long.toString((end_time - start_time) / reps)
                               + "ms)");

            if (showContents)
            {
               hexPrint(read_buf, 0, bank.getSize());
               System.out.println("");
            }
         }
         catch (Exception e)
         {
            System.out.println("Exception in reading raw: " + e
                               + "  TRACE: ");
            e.printStackTrace();
         }
      }

      if (!found_bank)
         System.out.println("XXXX Does not contain any memory bank's");
   }

   /**
    * Dump valid memory packets from general-purpose memory.
    * in the provided  MemoryContainer instance.
    *
    * @parameter owd device to check for memory banks.
    * @parameter showContents flag to indicate if the packet memory bank contents will
    *                      be displayed
    */
   public static void dumpDevicePackets (OneWireContainer owd,
                                         boolean showContents)
   {
      byte[]  read_buf, extra_buf;
      long    start_time, end_time;
      int     read_rslt;
      boolean found_bank = false;

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

                  if (showContents)
                  {
                     System.out.print("Packet " + pg + " (" + read_rslt
                                      + "): ");
                     hexPrint(read_buf, 0, read_rslt);
                     System.out.println("");

                     if (bank.hasExtraInfo())
                     {
                        System.out.print("Extra: ");
                        hexPrint(extra_buf, 0, bank.getExtraInfoLength());
                        System.out.println("");
                     }
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
               e.printStackTrace();

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
    * Dump pages from memory.
    * in the provided owd instance.
    *
    * @parameter owd device to check for memory banks.
    * @parameter showContents flag to indicate if the packet memory bank contents will
    *                      be displayed
    */
   public static void dumpDevicePages (OneWireContainer owd,
                                       boolean showContents)
   {
      byte[]  read_buf, extra_buf;
      long    start_time, end_time;
      int     read_rslt, reps, i, pg, numberPages;
      boolean found_bank = false, hasExtraInfo, hasPageAutoCRC, readContinue;

      // get the port names we can use and try to open, test and close each
      for (Enumeration bank_enum = owd.getMemoryBanks();
              bank_enum.hasMoreElements(); )
      {

         // get the next memory bank
         MemoryBank mb = ( MemoryBank ) bank_enum.nextElement();

         // check if has paged services
         if (!(mb instanceof PagedMemoryBank))
            continue;

         // cast to page bank 
         PagedMemoryBank bank = ( PagedMemoryBank ) mb;

         // found a memory bank
         found_bank = true;

         // display bank information
         displayBankInformation(bank);

         read_buf  = new byte [bank.getPageLength()];
         extra_buf = new byte [bank.getExtraInfoLength()];

         // get bank flags
         hasPageAutoCRC = bank.hasPageAutoCRC();
         hasExtraInfo   = bank.hasExtraInfo();
         numberPages    = bank.getNumberPages();

         // get overdrive going so not a factor in time tests
         try
         {
            bank.read(0, false, read_buf, 0, 1);
         }
         catch (Exception e){}

         // dynamically change number of reps
         reps = 1000 / (read_buf.length * bank.getNumberPages());

         if (owd.getMaxSpeed() == DSPortAdapter.SPEED_OVERDRIVE)
            reps *= 2;

         if ((reps == 0) || showContents)
            reps = 1;

         if (!showContents)
            System.out.print("[" + reps + "]");

         // start timer to time the dump of the bank contents         
         start_time = System.currentTimeMillis();

         for (i = 0; i < reps; i++)
         {

            // loop to read all of the pages in bank
            readContinue = false;

            for (pg = 0; pg < numberPages; pg++)
            {
               try
               {

                  // read a page (use the most verbose and secure method)
                  if (hasPageAutoCRC)
                  {
                     if (hasExtraInfo)
                        bank.readPageCRC(pg, readContinue, read_buf, 0,
                                         extra_buf);
                     else
                        bank.readPageCRC(pg, readContinue, read_buf, 0);
                  }
                  else
                  {
                     if (hasExtraInfo)
                        bank.readPage(pg, readContinue, read_buf, 0,
                                      extra_buf);
                     else
                        bank.readPage(pg, readContinue, read_buf, 0);
                  }

                  readContinue = true;

                  if (showContents)
                  {
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
               }
               catch (Exception e)
               {
                  System.out.println("Exception in reading page: " + e
                                     + "TRACE: ");
                  e.printStackTrace();

                  readContinue = false;
               }
            }
         }

         end_time = System.currentTimeMillis();

         System.out.println("     (time to read PAGES = "
                            + Long.toString((end_time - start_time) / reps)
                            + "ms)");
      }

      if (!found_bank)
         System.out.println(
            "XXXX Does not contain any general-purpose non-volatile page memory bank's");
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
}
