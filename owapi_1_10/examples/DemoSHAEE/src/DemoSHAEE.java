/*---------------------------------------------------------------------------
 * Copyright (C) 1999 Dallas Semiconductor Corporation, All Rights Reserved.
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
 * OneWireContainer33 to write blocks and packets and use different
 * features of the part.
 *
 * History:
 *
 * @version    0.00, 19 Dec 2000
 * @author     DS
 */

public class DemoSHAEE
{
   public static void main(String[] args)
   {
      byte[] data       = new byte[8];
      byte[] sn         = new byte[8];
      byte[] secret     = new byte[8];
      byte[] partialsec = new byte[8];
      byte[] memory     = new byte[32];
      byte[] indata     = new byte[32];
      byte[] hexstr     = new byte[32];
      byte[] family     = new byte[2];
      byte[] extra_info = new byte[20];
      Vector owd_vect   = new Vector(5);
      int i;
      int rslt,n=13,skip,page,addr,len;
      int cnt = 0;
      int reads = 0;
      int add;
      byte address;
      MemoryBank         bank;
      DSPortAdapter      adapter = null;
      OneWireContainer33 owd;
      boolean found      = false;
      boolean done       = false;
      boolean donebank   = false;

      family[0] = ( byte ) 0x33;
      family[1] = ( byte ) 0xB3;

      for(i=0; i<8; i++)
      {
         secret[i]     = ( byte ) 0xFF;
         data[i]       = ( byte ) 0xFF;
         partialsec[i] = ( byte ) 0x00;
      }

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         adapter.targetFamily(family);
         owd_vect = findAllDevices(adapter);
         // select a device
         owd = selectDevice(owd_vect);

         sn = owd.getAddress();

         do
         {
            // Main menu
            switch (menuSelect(mainMenu))
            {

               case QUIT :
                  n = 0;        //used to finish off the loop
                  done = true;
                  break;

               case PRNT_BUS_SECRET:  // Print Bus Master Secret
                  System.out.println();
                  System.out.println("The Current Bus Master Secret is:");
                  hexPrint(secret,0,8);
                  System.out.println();
                  break;

               case RD_WR_MEM:  // Read/Write Memory of Bank
                  // select a bank
                  bank = selectBank(owd);

                  // display bank information
                  displayBankInformation(bank);

                  // loop on bank menu
                  do
                  {
                     switch (menuSelect(bankMenu))
                     {

                        case BANK_INFO :
                           // display bank information
                           displayBankInformation(bank);
                           break;

                        case BANK_READ_BLOCK :
                           // read a block
                           System.out.print("Enter the address to start reading: ");

                           addr = getNumber(0, bank.getSize() - 1);

                           System.out.print("Enter the length of data to read: ");

                           len = getNumber(0, bank.getSize());

                           System.out.println();
                           dumpBankBlock(bank, addr, len);
                           break;

                        case BANK_READ_PAGE :
                           if (!(bank instanceof PagedMemoryBank))
                              System.out.print("Bank is not a 'PagedMemoryBank'");
                           else
                           {
                              System.out.print("Enter the page number to read: ");

                              page = getNumber(0, (( PagedMemoryBank ) bank).getNumberPages() - 1);

                              System.out.println();
                              dumpBankPage(owd, ( PagedMemoryBank ) bank, page);
                           }
                           break;

                        case BANK_READ_UDP :
                           if (!(bank instanceof PagedMemoryBank))
                              System.out.print("Bank is not a 'PagedMemoryBank'");
                           else
                           {
                              System.out.print("Enter the page number to read: ");

                              page = getNumber(0, (( PagedMemoryBank ) bank).getNumberPages() - 1);

                              System.out.println();
                              dumpBankPagePacket(( PagedMemoryBank ) bank, page);
                           }
                           break;

                        case BANK_WRITE_BLOCK :  // write a block
                           System.out.print("Enter the address to start writing: ");

                           addr = getNumber(0, bank.getSize() - 1);
                           data = getData(false);

                           bankWriteBlock(bank, data, addr);
                           break;

                        case BANK_WRITE_UDP :   // write a packet
                           if (!(bank instanceof PagedMemoryBank))
                              System.out.print("Bank is not a 'PagedMemoryBank'");
                           else
                           {
                              System.out.print("Enter the page number to write a UDP to: ");

                              page = getNumber(0, (( PagedMemoryBank ) bank).getNumberPages() - 1);
                              data = getData(false);

                              bankWritePacket(( PagedMemoryBank ) bank, data, page);
                           }
                           break;

                        case BANK_NEW_BANK :  // select a bank
                           bank = selectBank(owd);
                           break;

                        case BANK_MAIN_MENU :
                           donebank = true;
                           break;
                     }
                  }
                  while (!donebank);
                  donebank = false;
                  break;

               case FIRST_SECRET:  // Load First Secret
                  System.out.println();
                  System.out.println("Enter the 8 bytes of data to be written.");
                  System.out.println("AA AA AA AA AA AA AA AA  <- Example");

                  // get secret data
                  data = getData(true);

                  if(owd.loadFirstSecret(data, 0))
                  {
                     System.out.println("First Secret was Loaded.");
                     System.arraycopy(data, 0, secret, 0, 8);
                  }

                  break;

               case COMPUTE_NEXT:  // Compute Next Secret
                  System.out.println();
                  System.out.println("Enter the address for the page you want to calculate the next secret with");

                  // reading in address
                  add = getNumber(0,128);

                  System.out.println();
                  System.out.println("Enter the 8 byte partial secret.");

                  // reading the partial secret
                  partialsec = getData(true);

                  // computing next secret
                  owd.computeNextSecret(add, partialsec, 0);

                  System.out.println();
                  System.out.println("Next Secret Computed");
                  System.out.println();

                  break;

               case NEW_BUS_SECRET:  // Change Bus Master Secret
                  System.out.println();
                  System.out.println("Enter the 8 bytes of data to be written.");
                  System.out.println("AA AA AA AA AA AA AA AA  <- Example");

                  // get secret data
                  data = getData(true);

                  System.arraycopy(data, 0, secret, 0, 8);

                  System.out.println("Bus Master Secret Changed.");

                  owd.setContainerSecret(data, 0);

                  break;

               case LOCK_SECRET:  // Lock Secret
                  owd.writeProtectSecret();
                  System.out.println("Secret Locked.");

                  break;

               case NEW_CHALLENGE:  // New Challenge to Input
                  System.out.println();
                  System.out.println("Enter 8 bytes for the challenge");
                  System.out.println("AA AA AA AA AA AA AA AA  <- Example");

                  // get the challenge
                  data = getData(true);

                  owd.setChallenge(data, 0);

                  break;

               case WR_PRO_0THR3:  // Write-protect pages 0 to 3
                  owd.writeProtectAll();
                  System.out.println("Pages 0 to 3 write protected.");

                  break;

               case PAGE_1_EEPROM:  // Set page 1 to EPROM mode
                  owd.setEPROMModePageOne();
                  System.out.println("EPROM mode control activated for page 1.");

                  break;

               case WR_PRO_0:  // Write protect page 0
                  owd.writeProtectPageZero();
                  System.out.println("Page 0 Write-protected.");

                  break;

               default:
                  break;
            }

         }while(!done);

      }
      catch (Exception e)
      {
         System.out.println(e);
      }
      finally
      {
         if(adapter != null)
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
   public static byte[] getData (boolean eight_bytes)
   {
      byte[]  data     = null;
      boolean got_data = false;
      byte[]  zero     = {(byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00,
                          (byte)0x00,(byte)0x00,(byte)0x00,(byte)0x00};

      if (menuSelect(modeMenu) == MODE_TEXT)
      {
         if(!eight_bytes)
         {
            String tstr = getString(1);

            data = tstr.getBytes();
         }
         else
         {
            do
            {
               String tstr = getString(1);

               data = tstr.getBytes();

               if(data.length>8)
               System.out.println("Entry is too long, must be 8 bytes of data or less.");
            }
            while(data.length>8);

            if(data.length<8)
            {
               System.arraycopy(data,0,zero,0,data.length);
               data = zero;
            }
         }
      }
      else
      {
         if(eight_bytes)
         {
            do
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

               if(data.length>8)
                  System.out.println("Entry is too long, must be 8 bytes of data or less.");
            }
            while(data.length>8);

            if(data.length<8)
            {
               System.arraycopy(data,0,zero,0,data.length);
               data = zero;
            }
         }
         else
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

   /**
    * Read a page from a memory bank and print in hex
    *
    * @param  bank  PagedMemoryBank to read a page from
    * @param  pg  page to read
    */
   public static void dumpBankPage (OneWireContainer33 owd, PagedMemoryBank bank, int pg)
   {
      byte[] read_buf  = new byte [bank.getPageLength()];
      byte[] extra_buf = new byte [bank.getExtraInfoLength()];
      byte[] challenge = new byte [8];
      byte[] secret    = new byte [8];
      byte[] sernum    = new byte [8];
      boolean macvalid = false;

      try
      {

         // read a page (use the most verbose and secure method)
         if (bank.hasPageAutoCRC())
         {
            System.out.println("Using device generated CRC");

            if (bank.hasExtraInfo())
            {
               bank.readPageCRC(pg, false, read_buf, 0, extra_buf);

               owd.getChallenge(challenge, 0);
               owd.getContainerSecret(secret, 0);
               sernum = owd.getAddress();
               macvalid = owd.isMACValid(bank.getStartPhysicalAddress()+pg*bank.getPageLength(),
                                         sernum,read_buf,extra_buf,challenge,secret);
            }
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

            if(macvalid)
               System.out.println("Data validated with correct MAC.");
            else
               System.out.println("Data not validated because incorrect MAC.");
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
         adapter.setSpeed(DSPortAdapter.SPEED_REGULAR);

         // enumerate through all the 1-Wire devices and collect them in a vector
         for (Enumeration owd_enum = adapter.getAllDeviceContainers();
                 owd_enum.hasMoreElements(); )
         {
            System.out.println("one device found.");
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

   /**
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    *
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer33 selectDevice (Vector owd_vect)
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

      return ( OneWireContainer33 ) owd_vect.elementAt(select);
   }

   //--------
   //-------- Menus
   //--------
   static final String[] mainMenu           = { "Main Menu",
                                                "(0)  Read/Write Memory Bank",
                                                "(1)  Load First Secret",
                                                "(2)  Compute Next Secret",
                                                "(3)  Change Bus Master Secret",
                                                "(4)  Lock Secret",
                                                "(5)  Input new challenge for Read Authenticate",
                                                "(6)  Write Protect page 0-3",
                                                "(7)  Set Page 1 to EEPROM mode",
                                                "(8)  Write Protect Page 0",
                                                "(9)  Print Current Bus Master Secret",
                                                "(10) Quit" };
   static final int      RD_WR_MEM          = 0;
   static final int      FIRST_SECRET       = 1;
   static final int      COMPUTE_NEXT       = 2;
   static final int      NEW_BUS_SECRET     = 3;
   static final int      LOCK_SECRET        = 4;
   static final int      NEW_CHALLENGE      = 5;
   static final int      WR_PRO_0THR3       = 6;
   static final int      PAGE_1_EEPROM      = 7;
   static final int      WR_PRO_0           = 8;
   static final int      PRNT_BUS_SECRET    = 9;
   static final int      QUIT               = 10;

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

   static final String[] modeMenu           = { "Data Entry Mode",
                                                "(0) Text (single line)",
                                                "(1) Hex (XX XX XX XX ...)" };
   static final int      MODE_TEXT          = 0;
   static final int      MODE_HEX           = 1;
}

