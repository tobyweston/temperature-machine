public interface AuthenticationConstants
{
   /** Debug message flag */
   static final boolean DEBUG = false;

   /** Default port for NetAdapter TCP/IP connection */
   static final int DEFAULT_PORT = 6161;

   /** default secret for authentication with the server */
   static final String DEFAULT_SECRET = "Adapter Secret Default";

   /*--------------------------------*/
   /*----- Method Return codes -----*/
   /*--------------------------------*/
   static final byte RET_SUCCESS = (byte)0xFF;
   static final byte RET_FAILURE = (byte)0xF0;

   /** Commands sent over the network */
   static final byte NO_ACTION     = 0x00;
   static final byte START_OF_AUTH = (byte) 0xAA;
   static final byte END_OF_AUTH = (byte) 0xA0;
   static final byte CHALLENGE_FOR_VERIFICATION = (byte) 0xA1;
   static final byte VERIFY_USER_DATA = (byte) 0xA2;
   static final byte VERIFY_TRANSACTION_DATA = (byte) 0xA3;


   /** Failures */
   static final byte COPROCESSOR_FAILURE = (byte) 0xC0;
   static final byte COPR_COMPUTE_CHALLENGE_FAILED = (byte) 0xC1;

   /** An inner utility class for coupling Socket with I/O streams */
   public static final class Connection
   {
      /** socket to host */
      public java.net.Socket sock = null;
      /** input stream from socket */
      public java.io.DataInputStream input = null;
      /** output stream from socket */
      public java.io.DataOutputStream output = null;
   }

   /** private instance for an empty connection, basically it's a NULL object
    *  that's safe to synchronize on. */
   public static final Connection EMPTY_CONNECTION = new Connection();
}   