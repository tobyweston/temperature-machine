/*---------------------------------------------------------------------------
 * Copyright (C) 2002 Dallas Semiconductor Corporation, All Rights Reserved.
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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Vector;

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.application.sha.*;
import com.dalsemi.onewire.OneWireAccessProvider;

/**
 * Starts the host component for NetAdapter clients on the local machine.
 * If no options are specified, the default adapter for this machine is used
 * and the host is launched as a multi-threaded server using the defaults.
 *
 */
public class AuthenticateUser
{
   static final String strUsage =
"Starts the host component for NetAdapter clients on the local machine.\n" +
"If no options are specified, the default adapter for this machine is used\n" +
"and the host is launched as a multi-threaded server using the defaults:\n" +
"\n" +
"  Host Listen Port: " + NetAdapterConstants.DEFAULT_PORT + "\n" +
"\n" +
"syntax: java StartNetAdapterHost <options>\n" +
"\n" +
"Options:\n" +
"  -props                    Pulls all defaults from the onewire.properties\n" +
"                            file rather than using the defaults set in\n" +
"                            com.dalsemi.onewire.adapter.NetAdapterConstants.\n" +
"  -adapterName STRING       Selects the Adapter to use for the host.\n" +
"  -adapterPort STRING       Selects the Adapter port to use for the host.\n" +
"  -listenPort NUM           Sets the host's listening port for incoming\n" +
"                            socket connections.\n" +
"  -secret STRING            Sets the shared secret for authenticating incoming\n" +
"                            client connections.\n";

   public static void usage()
   {
      System.out.println();
      System.out.println(strUsage);
      System.exit(1);
   }

   public static void main(String[] args)
      throws Exception
   {
      String adapterName = null, adapterPort = null;
      int listenPort = AuthenticationConstants.DEFAULT_PORT;
      boolean multithread = true;
      String secret = AuthenticationConstants.DEFAULT_SECRET;
      byte cmd = AuthenticationConstants.NO_ACTION;  // 0x00
      byte[] tempROM = new byte[]
           { (byte)0x18,(byte)0x20,(byte)0xAF,(byte)0x02,
             (byte)0x00,(byte)0x00,(byte)0x00,(byte)0xE7 };

      OneWireContainer owcuser;
      SHAiButtonUser   ibuser;
     	long tmpID = -1;
     	byte[] shachlg = new byte[3];
     	byte[] accountData = new byte[32];
     	byte[] mac = new byte[20];
     	byte[] coprBindCode = new byte[7];
     	byte[] coprBindData = new byte[32];
     	byte[] fullBindCode = new byte[15];
     	int wcc = 0;

      boolean useProperties = false;

      if(args.length>0)
      {
         try
         {
            // check to see if they are looking for help
            char c = args[0].charAt(1);
            if(args[0].charAt(0)!='-' || c=='h' || c=='H' || c=='?')
               usage();

            // do one pass to see if we're supposed to use the properties file
            for(int i=0; !useProperties && i<args.length; i++)
               useProperties = (args[i].equalsIgnoreCase("-props"));

            // if we found -props, load all the defauls from onewire.properties
            // that can be found there.
            if(useProperties)
            {
               String test = OneWireAccessProvider.getProperty("onewire.adapter.default");
               if(test!=null)
                  adapterName = test;

               test = OneWireAccessProvider.getProperty("onewire.port.default");
               if(test!=null)
                  adapterPort = test;

               test = OneWireAccessProvider.getProperty("NetAdapter.ListenPort");
               if(test!=null)
                  listenPort = Integer.parseInt(test);

               test = OneWireAccessProvider.getProperty("NetAdapter.Secret");
               if(test!=null)
                  secret = test;
            }

            // get the other propertie values from the command-line
            // these will override the defaults
            for(int i=0; i<args.length; i++)
            {
               String arg = args[i];
               if(arg.charAt(0)!='-')
                  usage();

               if(arg.equalsIgnoreCase("-adapterName"))
               {
                  adapterName = args[++i];
               }
               else if(arg.equalsIgnoreCase("-adapterPort"))
               {
                  adapterPort = args[++i];
               }
               else if(arg.equalsIgnoreCase("-listenPort"))
               {
                  listenPort = Integer.parseInt(args[++i]);
               }
               else if(arg.equalsIgnoreCase("-secret"))
               {
                  secret = args[++i];
               }
               else
               {
                  System.out.println("Invalid option: " + arg);
                  usage();
               }
            }
         }
         catch(Exception e)
         {
            System.out.println("Error parsing arguments: " + e.getMessage());
            System.out.println("");
            usage();
         }
      }

      // get the appropriate adapter.
      DSPortAdapter adapter;
      if(adapterName==null || adapterPort==null)
         adapter = OneWireAccessProvider.getDefaultAdapter();
      else
         adapter = OneWireAccessProvider.getAdapter(adapterName, adapterPort);


      // --------------------------------------------------------------
      // Search for the ibutton
      // --------------------------------------------------------------
      boolean next = false;
      try
      {
        	next = adapter.findFirstDevice();
        	for(; tmpID==-1;next=adapter.findNextDevice())
        	{
        		tmpID = adapter.getAddressAsLong();

     		   adapter.getAddress(tempROM);
         }

         if(tmpID == -1)
         {
           	IOHelper.writeLine("No buttons found!");
           	System.exit(1);
         }
      }
      catch(Exception e)
      {
        	IOHelper.writeLine("Adapter error while searching.");
        	System.exit(1);
      }

      System.out.println("Enter the Host server you want to connect to.");
      String host = IOHelper.readLine();
      java.net.Socket s = new java.net.Socket(host,listenPort);
      AuthenticationConstants.Connection conn =
         new AuthenticationConstants.Connection();
      conn.sock = s;
      conn.input = new DataInputStream(s.getInputStream());
      conn.output
             = new DataOutputStream(new BufferedOutputStream(
                                        s.getOutputStream()));

      // we need to authenticate ourselves
      // using the challenge from the server.
      byte[] chlg = new byte[8];
      conn.input.read(chlg, 0, 8);

      // compute the crc of the secret and the challenge
      int crc = CRC16.compute(secret.getBytes(), 0);
      crc = CRC16.compute(chlg, crc);
      // and send it back to the server

      conn.output.writeInt(crc);
      conn.output.flush();

      // RETURN_SUCCESS = 0xFF
      if(conn.input.readByte() != AuthenticationConstants.RET_SUCCESS)
      {
      	System.out.println(conn.input.readUTF());
      }
      else
      {
         System.out.println();
         System.out.println("Connected to Host and now receiving bind code,");
         System.out.println("bind data and file name.");
         System.out.println();

      	while(conn.sock!=null)
      	{
      		conn.output.writeByte(AuthenticationConstants.START_OF_AUTH);
      		conn.output.flush();

            // write the ROM so the host knows what part is being used
         	conn.output.write(tempROM);
         	conn.output.flush();

            // receiving the bind code and the bind data from the
            // network coprocessor
            conn.input.read(coprBindCode,0,7);
            conn.input.read(coprBindData,0,32);

            // reading in the bind code, file name and extension
            byte[] fileName = new byte[4];
            byte fileNameExt;
            conn.input.read(fileName,0,4);
            fileNameExt = conn.input.readByte();

         	// used to make sure owcuser is setup
         	if(tempROM[0] == 0x18)
         	{
         		owcuser = new OneWireContainer18(adapter,tempROM);
         		ibuser  = new SHAiButtonUser18(coprBindCode, fileName,
         		                              (int) fileNameExt,
         		                              (OneWireContainer18)owcuser);
         	}
         	else
         	{
         		owcuser = new OneWireContainer33(adapter,tempROM);
         		ibuser  = new SHAiButtonUser33(coprBindCode, fileName,
                                             (int) fileNameExt,
                                             (OneWireContainer33)owcuser);
         	}
         	owcuser.setupContainer(adapter,tmpID);

            boolean done = false;
            boolean shaChlgRead  = false;
            boolean readAcctData = false;
            while(!done)
            {
               cmd = conn.input.readByte();

               switch(cmd)
               {
                  case AuthenticationConstants.CHALLENGE_FOR_VERIFICATION:
                     System.out.println("Receiving challenge for " +
                                        "authentication");
                     System.out.println();
                     // reading the challenge that is to be
                     // used for authentication
                     conn.input.read(shachlg,0,3);
                     shaChlgRead = true;
                     break;

                  case AuthenticationConstants.VERIFY_USER_DATA:
                     System.out.println("Sending the user verification " +
                                        "data.");
                     System.out.println();

                     if(readAcctData)
                     {
                        conn.output.writeInt(wcc);
                        conn.output.flush();
                        conn.output.write(accountData);
                        conn.output.flush();
                        conn.output.write(mac);
                        conn.output.flush();

                        ibuser.getFullBindCode(fullBindCode,0);
                        conn.output.write(fullBindCode);
                        conn.output.flush();
                     }
                     else if(shaChlgRead)
                     {
                        // read the account data and the mac
                        wcc = ibuser.readAccountData(shachlg, 0, accountData, 0,
                                                     mac, 0);

                        conn.output.writeInt(wcc);
                        conn.output.flush();
                        conn.output.write(accountData);
                        conn.output.flush();
                        conn.output.write(mac);
                        conn.output.flush();

                        ibuser.getFullBindCode(fullBindCode,0);
                        conn.output.write(fullBindCode);
                        conn.output.flush();

                        readAcctData = true;
                     }
                     break;

                  case AuthenticationConstants.VERIFY_TRANSACTION_DATA:
                     System.out.println("Sending the verify transaction " +
                                        "data.");
                     System.out.println();

                     if(readAcctData)
                     {
                        conn.output.write(accountData);
                        conn.output.flush();

                        conn.output.writeInt(wcc);
                        conn.output.flush();

                        conn.output.writeInt(ibuser.getAccountPageNumber());
                        conn.output.flush();
                     }
                     else if(shaChlgRead)
                     {
                        // read the account data and the mac
                        wcc = ibuser.readAccountData(shachlg, 0, accountData, 0,
                                                     mac, 0);

                        conn.output.write(accountData);
                        conn.output.flush();
                        conn.output.writeInt(wcc);
                        conn.output.flush();
                        conn.output.writeInt(ibuser.getAccountPageNumber());
                        conn.output.flush();

                        readAcctData = true;
                     }
                     break;

                  case AuthenticationConstants.END_OF_AUTH:
                     System.out.println("End of authentication reached.");
                     done = true;
                     break;

                  default:
                     break;
               }
            }

            conn.sock.close();
         	conn.sock = null;
         	conn.input = null;
         	conn.output = null;
      	}
      }
   }

   public static byte[] getBytes(int cnt)
   {
      IOHelper.writeLine("   1 HEX");
      IOHelper.writeLine("   2 ASCII");
      System.out.print("  ? ");
      int choice = IOHelper.readInt(2);

      if(choice==1)
         return IOHelper.readBytesHex(cnt,0x00);
      else
         return IOHelper.readBytesAsc(cnt,0x20);
   }
}