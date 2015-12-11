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

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;
import com.dalsemi.onewire.utils.*;
import com.dalsemi.onewire.application.sha.*;

/**
 * Private inner class for servicing new connections.
 * Can be run in it's own thread or in the same thread.
 */
public class AuthenticationHost implements Runnable
{
	/**
    * For fast 0xFF fills of byte arrays
    */
   private static final byte[] ffBlock
      = new byte[] {
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF,
          (byte)0x0FF,(byte)0x0FF,(byte)0x0FF,(byte)0x0FF
       };

   /**
    * The connection that is being serviced.
    */
   private AuthenticationConstants.Connection conn;

   /**
    * The random variable for the connection verification
    */
   Random rand;

   /**
    * This is the secret for the connection
    */
   String secret;

   /**
    * The coprocessor information for remote authentications
    */
   SHAiButtonCopr copr;

   /**
    * The server socket for connections
    */
   ServerSocket serverSock;

   /**
    * Constructor for socket servicer.  Creates the input and output
    * streams and send's the version of this host to the client
    * connection.
    */
   public AuthenticationHost(SHAiButtonCopr copr, int listenPort, String secret)
      throws IOException
   {
   	// Random object for the challenge
   	rand = new Random();

   	// Setting the secret for connections
   	this.secret = secret;

   	// The coprocessor data for authentication
   	this.copr = copr;

      // The socket for connections
      serverSock = new ServerSocket(listenPort);
   }

   public void run()
   {
      byte[] SNum = new byte[8];
      byte[] data = new byte[7];

      try
      {
         for(;;)
      	{
				Socket sock = null;
				sock = serverSock.accept();

            // set socket timeout to 10 seconds
            sock.setSoTimeout(10000);

            // create the connection object
            conn = new AuthenticationConstants.Connection();
            conn.sock = sock;
            conn.input = new DataInputStream(conn.sock.getInputStream());
            conn.output = new DataOutputStream(new BufferedOutputStream(
                                                 conn.sock.getOutputStream()));


            // authenticate the client
            byte[] chlg = new byte[8];
            rand.nextBytes(chlg);
            conn.output.write(chlg);
            conn.output.flush();

            // compute the crc of the secret and the challenge
            int crc = CRC16.compute(secret.getBytes(), 0);
            crc = CRC16.compute(chlg, crc);
            int answer = conn.input.readInt();

            if(answer!=crc)
            {
               conn.output.writeByte(AuthenticationConstants.RET_FAILURE);
               conn.output.writeUTF("Client Authentication Failed");
               conn.output.flush();
               throw new IOException("authentication failed");
            }
            else
            {
               conn.output.writeByte(AuthenticationConstants.RET_SUCCESS);
               conn.output.flush();
            }

      		if(authenticate(SNum,data))
      		{
      		   System.out.println();
      		   System.out.println("Remote Authentication Successful for part: ");
      		   IOHelper.writeBytesHex(SNum,0,8);
      		   System.out.println("With the following verification data:");
      		   IOHelper.writeBytes(data);
      		   System.out.println();
      		}
      		else
      		{
      		   System.out.println("Remote Authentication was " +
                 	                "unseccessful for part: ");
               IOHelper.writeBytesHex(SNum,0,8);
            }
      	}
      }
      catch(Exception ioe)
      {
      	if(AuthenticationConstants.DEBUG)
      	   ioe.printStackTrace();
      }
   }

   /**
    * Run method for socket Servicer.
    */
   public boolean authenticate(byte[] SNum, byte[] data)
      throws OneWireException, OneWireIOException, IOException
   {
     	byte[] accountData  = new byte[32];
     	byte[] fullBindCode = new byte[15];
     	byte[] ver_data     = new byte[7];
     	byte[] mac  = new byte[20];
     	byte[] coprBindCode = new byte[7];
     	byte[] coprBindData = new byte[32];
     	byte lastError = 0x00;
     	byte authCmd = 0x00;
     	boolean authenticated = false;

      try
      {
         while(conn.sock!=null)
         {
     			byte cmd = 0x00;

     			cmd = conn.input.readByte();

            if(cmd == AuthenticationConstants.START_OF_AUTH)
            {
               // get the bytes to block
               conn.input.read(SNum, 0, 8);

               // send the bind code
               coprBindCode = copr.getBindCode();
               conn.output.write(coprBindCode);
               conn.output.flush();

               coprBindData = copr.getBindData();
               conn.output.write(coprBindData);
               conn.output.flush();

               // Send the file name and file name extension of the file
               byte[] fileName = new byte[4];
               byte fileNameExt;
               copr.getFilename(fileName,0);
               fileNameExt = copr.getFilenameExt();

               conn.output.write(fileName);
               conn.output.flush();

               conn.output.write(fileNameExt);
               conn.output.flush();

               if(SNum[0] == 0x18)
               {
                 	authCmd = OneWireContainer18.VALIDATE_DATA_PAGE;
               }
               else if((SNum[0] == 0x33) || (SNum[0] == 0xB3))
               {
                 	authCmd = OneWireContainer18.AUTH_HOST;
               }

               // Verifing the User
               lastError = verifyUserNet(copr, authCmd);

               // verify Transaction Data
               if((verifyTransactionDataNet(copr, data, SNum)
                   == 0x00) && (lastError == 0x00))
               {
                  authenticated =  true;
               }

               conn.output.writeByte(AuthenticationConstants.END_OF_AUTH);
               conn.output.flush();
               conn.sock.close();
            }
         }
      }
      catch(IOException ioe)
      {
         if(AuthenticationConstants.DEBUG)
            ioe.printStackTrace();
         conn.sock.close();
      }

      return authenticated;
   }

   private byte verifyUserNet(SHAiButtonCopr copr, byte authCmd)
      throws OneWireException, OneWireIOException, IOException
   {
     	byte lastError = 0x00;
     	int  wcc = 0;
     	byte[] chlg = new byte[3];
     	byte[] accountData  = new byte[32];
     	byte[] mac          = new byte[20];
     	byte[] fullBindCode = new byte[15];
     	byte[] coprBindCode = new byte[7];
     	byte[] coprBindData = new byte[32];
     	byte[] scratchpad   = new byte[32];

      if(!copr.generateChallenge(0, chlg, 0))
      {
         lastError = AuthenticationConstants.COPR_COMPUTE_CHALLENGE_FAILED;
      }

      conn.output.writeByte(AuthenticationConstants.CHALLENGE_FOR_VERIFICATION);
      conn.output.flush();
      conn.output.write(chlg,0,chlg.length);
      conn.output.flush();

      conn.output.writeByte(AuthenticationConstants.VERIFY_USER_DATA);
      conn.output.flush();

      wcc = conn.input.readInt();
      conn.input.read(accountData,0,accountData.length);
      conn.input.read(mac,0,mac.length);
      conn.input.read(fullBindCode,0,fullBindCode.length);

      if(wcc<0)
      {
         System.arraycopy(ffBlock, 0, scratchpad, 8, 4);
      }
      else
      {
         //copy the write cycle counter into scratchpad
         Convert.toByteArray(wcc, scratchpad, 8, 4);
      }

      //get the user address and page num from fullBindCode
      System.arraycopy(fullBindCode, 4, scratchpad, 12, 8);

      //set the same challenge bytes
      System.arraycopy(chlg, 0, scratchpad, 20, 3);

      if(!copr.verifyAuthentication(fullBindCode,
                                    accountData,
                                    scratchpad, mac,
                                    authCmd))
      {
         lastError = AuthenticationConstants.COPROCESSOR_FAILURE;
      }

      return lastError;
   }

   private byte verifyTransactionDataNet(SHAiButtonCopr copr, byte[] ver_data,
                                         byte[] SNum)
      throws OneWireException, OneWireIOException, IOException
   {
     	byte lastError = 0x00;
     	int  wcc = 0;
     	byte[] accountData  = new byte[32];
     	byte[] ver_mac      = new byte[20];
     	byte[] scratchpad   = new byte[32];

      conn.output.writeByte(AuthenticationConstants.VERIFY_TRANSACTION_DATA);
      conn.output.flush();

      conn.input.read(accountData,0,accountData.length);
      wcc = conn.input.readInt();

      //get the user's verification data
      System.arraycopy(accountData,22,ver_data,0,7);

      //get the mac from the account data page
      System.arraycopy(accountData, 2,
                       ver_mac, 0, 20);

      //now lets reset the mac
      copr.getInitialSignature(accountData, 2);

      //and reset the CRC
      accountData[30] = (byte)0;
      accountData[31] = (byte)0;

      //now we also need to get things like wcc,
      //user_page_number, user ID
      if(wcc<0)
      {
         //has no write cycle counter
         System.arraycopy(ffBlock, 0, scratchpad, 8, 4);
      }
      else
      {
         //copy the write cycle counter into scratchpad
         Convert.toByteArray(wcc, scratchpad, 8, 4);
      }

      scratchpad[12] = (byte)conn.input.readInt();

      System.arraycopy(SNum,0,scratchpad,13,7);

      copr.getSigningChallenge(scratchpad, 20);

      if(!copr.verifySignature(accountData, scratchpad,
                               ver_mac))
      {
         lastError = AuthenticationConstants.COPROCESSOR_FAILURE;
      }

      return lastError;
   }
}