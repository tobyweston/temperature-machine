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
 * OneWireContainer29 to write blocks and packets and use different
 * features of the part.
 *
 * History:
 *
 * @version    0.00, 19 Dec 2000
 * @author     JPE
 */
public class DS2408Demo
{
   public static void main(String[] args)
   {
      OneWireContainer29 owd;
      byte[] data       = new byte[8];
      byte[] sn         = new byte[8];
      byte[] memory     = new byte[32];
      byte[] indata     = new byte[32];
      byte[] hexstr     = new byte[32];
      byte[] state      = new byte[3];
      byte[] register   = new byte[3];
      Vector owd_vect   = new Vector(5);
      int channel;
      int i;
      int rslt,n=13,skip,page,addr,len;
      int cnt = 0;
      int reads = 0;
      int add;
      byte address;
      MemoryBank         bank;
      DSPortAdapter      adapter = null;
      boolean stateRead    = false;
      boolean registerRead = false;
      boolean found        = false;
      boolean done         = false;
      boolean donebank     = false;

      for(i=0;i<8;i++)
         sn[i] = (byte) 0x00;

      try
      {
         adapter = OneWireAccessProvider.getDefaultAdapter();

         // get exclusive use of adapter
         adapter.beginExclusive(true);

         adapter.targetFamily(41);
         owd_vect = findAllDevices(adapter);

         // select a device
         owd = selectDevice(owd_vect);

         do
         {

            // Main menu
            switch (menuSelect(mainMenu))
            {

               case QUIT :
                  n = 0;        //used to finish off the loop
                  done = true;
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

                        case BANK_WRITE_BLOCK :  // write a block
                           System.out.print("Enter the address to start writing: ");

                           addr = getNumber(0, bank.getSize() - 1);
                           data = getData(false);

                           bankWriteBlock(bank, data, addr);
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

               case GET_LEVEL:  // Get level for certain channel
                  if(!stateRead)
                  {
                     state = owd.readDevice();
                     stateRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to check the level on?");
                  System.out.println();

                  channel = getNumber(0,7);

                  if(owd.getLevel(channel,state))
                     System.out.println("The level sensed on channel " + channel + " is high.");
                  else
                     System.out.println("The level sensed on channel " + channel + " is low.");

                  break;

               case GET_LATCH_STATE:  // Get Latch State
                  if(!stateRead)
                  {
                     state = owd.readDevice();
                     stateRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to check the latch state on?");
                  System.out.println();

                  channel = getNumber(0,7);

                  if(owd.getLatchState(channel,state))
                     System.out.println("The state of the latch on channel " + channel + " is on.");
                  else
                     System.out.println("The state of the latch on channel " + channel + " is off.");

                  break;

               case GET_SENSED_ACTIVITY:  // Get sensed activity
                  if(!stateRead)
                  {
                     state = owd.readDevice();
                     stateRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to check for activity?");
                  System.out.println();

                  channel = getNumber(0,7);

                  if(owd.getSensedActivity(channel,state))
                     System.out.println("Activity was detected on channel " + channel);
                  else
                     System.out.println("No activity was detected on channel " + channel);

                  break;

               case CLEAR_ACTIVITY:  // Clear Activity
                  owd.clearActivity();
                  break;

               case SET_LATCH_STATE_ON:  // Set latch state on
                  if(!stateRead)
                  {
                     state = owd.readDevice();
                     stateRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to set on?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setLatchState(channel,true,false,state);
                  owd.writeDevice(state);

                  break;

               case SET_LATCH_STATE_OFF:  // Set latch state off
                  if(!stateRead)
                  {
                     state = owd.readDevice();
                     stateRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to turn off?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setLatchState(channel,false,false,state);
                  owd.writeDevice(state);

                  break;

               case CURRENT_STATE:  // Current state
                  state = owd.readDevice();
                  stateRead = true;

                  System.out.println("The following is the current level of the channels.");
                  for(i=0;i<8;i++)
                  {
                     if(owd.getLevel(i,state))
                        System.out.println(i + " is high.");
                     else
                        System.out.println(i + " is low.");
                  }

                  System.out.println();
                  System.out.println("The following are the latch states of the channels.");
                  for(i=0;i<8;i++)
                  {
                     if(owd.getLatchState(i,state))
                        System.out.println(i + " is on.");
                     else
                        System.out.println(i + " is off.");
                  }

                  System.out.println();
                  System.out.println("The following is the activity of the channels.");
                  for(i=0;i<8;i++)
                  {
                     if(owd.getSensedActivity(i,state))
                        System.out.println(i + " Activity");
                     else
                        System.out.println(i + " No activity");
                  }

                  break;

               case SET_RESET_ON:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.setResetMode(register,true);
                  owd.writeRegister(register);

                  break;

               case SET_RESET_OFF:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.setResetMode(register,false);
                  owd.writeRegister(register);

                  break;

               case GET_VCC:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  if(owd.getVCC(register))
                     System.out.println("VCC is powered with 5V.");
                  else
                     System.out.println("VCC is grounded.");

                  break;

               case CLEAR_POWER_ON_RESET:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.clearPowerOnReset(register);
                  owd.writeRegister(register);

                  break;

               case OR_CONDITIONAL_SEARCH:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.orConditionalSearch(register);
                  owd.writeRegister(register);

                  break;

               case AND_CONDITIONAL_SEARCH:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.andConditionalSearch(register);
                  owd.writeRegister(register);

                  break;

               case PIO_CONDITIONAL_SEARCH:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.pioConditionalSearch(register);
                  owd.writeRegister(register);

                  break;

               case ACTIVITY_CONDITIONAL_SEARCH:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  owd.activityConditionalSearch(register);
                  owd.writeRegister(register);

                  break;

               case SET_CHANNEL_MASK:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to set on?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setChannelMask(channel,true,register);
                  owd.writeRegister(register);

                  break;

               case UNSET_CHANNEL_MASK:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to turn off?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setChannelMask(channel,false,register);
                  owd.writeRegister(register);

                  break;

               case SET_CHANNEL_POLARITY:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to set the polarity on?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setChannelPolarity(channel,true,register);
                  owd.writeRegister(register);

                  break;

               case UNSET_CHANNEL_POLARITY:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to turn off the polarity?");
                  System.out.println();

                  channel = getNumber(0,7);

                  owd.setChannelPolarity(channel,false,register);
                  owd.writeRegister(register);

                  break;

               case GET_CHANNEL_MASK:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to check?");
                  System.out.println();

                  channel = getNumber(0,7);

                  if(owd.getChannelMask(channel,register))
                     System.out.println("Channel " + channel + " is masked.");
                  else
                     System.out.println("Channel " + channel + " is not masked.");

                  break;

               case GET_CHANNEL_POLARITY:
                  if(!registerRead)
                  {
                     register = owd.readRegister();
                     registerRead = true;
                  }

                  System.out.println();
                  System.out.println("Which channel would you like to check?");
                  System.out.println();

                  channel = getNumber(0,7);

                  if(owd.getChannelPolarity(channel,register))
                     System.out.println("Channel " + channel + " polarity is set.");
                  else
                     System.out.println("Channel " + channel + " polarity is not set.");

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
    * Create a menu from the provided OneWireContainer
    * Vector and allow the user to select a device.
    *
    * @param  owd_vect vector of devices to choose from
    *
    * @return OneWireContainer device selected
    */
   public static OneWireContainer29 selectDevice (Vector owd_vect)
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

      return ( OneWireContainer29 ) owd_vect.elementAt(select);
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

   //--------
   //-------- Menus
   //--------
   static final String[] mainMenu           = { "Main Menu",
                                                "(0)  Read/Write Memory Bank",
                                                "(1)  Get Channel Level",
                                                "(2)  Get Latch State",
                                                "(3)  Get Sensed Activity",
                                                "(4)  Clear Activity",
                                                "(5)  Set Latch State On",
                                                "(6)  Set Latch State Off",
                                                "(7)  The current state of the DS2408",
                                                "(8)  Set the reset pin",
                                                "(9)  Turn off the reset pin",
                                                "(10) Get VCC",
                                                "(11) Clear Power On Reset",
                                                "(12) Set 'OR' for Conditional Search",
                                                "(13) Set 'AND' for Conditional Search",
                                                "(14) Set PIO as input for Conditional Search",
                                                "(15) Set Activity latches as input for Conditional Search",
                                                "(16) Set Channel Mask",
                                                "(17) Turn off Channel Mask",
                                                "(18) Set Channel Polarity",
                                                "(19) Turn off Channel Polarity",
                                                "(20) Get Channel Mask",
                                                "(21) Get Channel Polarity",
                                                "(22) Quit"};
   static final int      RD_WR_MEM                   = 0;
   static final int      GET_LEVEL                   = 1;
   static final int      GET_LATCH_STATE             = 2;
   static final int      GET_SENSED_ACTIVITY         = 3;
   static final int      CLEAR_ACTIVITY              = 4;
   static final int      SET_LATCH_STATE_ON          = 5;
   static final int      SET_LATCH_STATE_OFF         = 6;
   static final int      CURRENT_STATE               = 7;
   static final int      SET_RESET_ON                = 8;
   static final int      SET_RESET_OFF               = 9;
   static final int      GET_VCC                     = 10;
   static final int      CLEAR_POWER_ON_RESET        = 11;
   static final int      OR_CONDITIONAL_SEARCH       = 12;
   static final int      AND_CONDITIONAL_SEARCH      = 13;
   static final int      PIO_CONDITIONAL_SEARCH      = 14;
   static final int      ACTIVITY_CONDITIONAL_SEARCH = 15;
   static final int      SET_CHANNEL_MASK            = 16;
   static final int      UNSET_CHANNEL_MASK          = 17;
   static final int      SET_CHANNEL_POLARITY        = 18;
   static final int      UNSET_CHANNEL_POLARITY      = 19;
   static final int      GET_CHANNEL_MASK            = 20;
   static final int      GET_CHANNEL_POLARITY        = 21;
   static final int      QUIT                        = 22;

   static final String[] bankMenu           =
   {
      "Bank Operation Menu", "(0) Get Bank information",
      "(1) Read Block", "(2) Write Block",
      "(3) GOTO MemoryBank Menu", "(4) GOTO MainMenu"
   };
   static final int      BANK_INFO          = 0;
   static final int      BANK_READ_BLOCK    = 1;
   static final int      BANK_WRITE_BLOCK   = 2;
   static final int      BANK_NEW_BANK      = 3;
   static final int      BANK_MAIN_MENU     = 4;

   static final String[] modeMenu           = { "Data Entry Mode",
                                                "(0) Text (single line)",
                                                "(1) Hex (XX XX XX XX ...)" };
   static final int      MODE_TEXT          = 0;
   static final int      MODE_HEX           = 1;
}

