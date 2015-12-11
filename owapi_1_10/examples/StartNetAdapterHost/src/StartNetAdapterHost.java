import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.OneWireAccessProvider;

/**
 * Starts the host component for NetAdapter clients on the local machine.
 * If no options are specified, the default adapter for this machine is used
 * and the host is launched as a multi-threaded server using the defaults.
 *
 */
public class StartNetAdapterHost
{
   static final String strUsage =
"Starts the host component for NetAdapter clients on the local machine.\r\n" +
"If no options are specified, the default adapter for this machine is used\r\n" +
"and the host is launched as a multi-threaded server using the defaults:\r\n" +
"\r\n" +
"  Host Listen Port: " + NetAdapterConstants.DEFAULT_PORT + "\r\n" +
"  Multithreaded Host: Enabled\r\n" +
"  Shared Secret: '" + NetAdapterConstants.DEFAULT_SECRET + "'\r\n" +
"  Multicast: Enabled\r\n" +
"  Multicast Port: " + NetAdapterConstants.DEFAULT_MULTICAST_PORT + "\r\n" +
"  Multicast Group: " + NetAdapterConstants.DEFAULT_MULTICAST_GROUP + "\r\n" +
"\r\n" +
"syntax: java StartNetAdapterHost <options>\r\n" +
"\r\n" +
"Options:\r\n" +
"  -props                    Pulls all defaults from the onewire.properties\r\n" +
"                            file rather than using the defaults set in\r\n" +
"                            com.dalsemi.onewire.adapter.NetAdapterConstants.\r\n" +
"  -adapterName STRING       Selects the Adapter to use for the host.\r\n" +
"  -adapterPort STRING       Selects the Adapter port to use for the host.\r\n" +
"  -listenPort NUM           Sets the host's listening port for incoming\r\n" +
"                            socket connections.\r\n" +
"  -multithread [true|false] Sets whether or not the hosts launches a new\r\n" +
"                            thread for every incoming client.\r\n" +
"  -secret STRING            Sets the shared secret for authenticating incoming\r\n" +
"                            client connections.\r\n" +
"  -multicast [true|false]   Enables/Disables the multicast listener. If\r\n" +
"                            disabled, clients will not be able to\r\n"+
"                            automatically discover this host.\r\n" +
"  -multicastPort NUM        Set the port number for receiving packets.\r\n" +
"  -multicastGroup STRING    Set the group for multicast sockets.  Must be in\r\n" +
"                            the range of '224.0.0.0' to '239.255.255.255'.\r\n";

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
      int listenPort = NetAdapterConstants.DEFAULT_PORT;
      boolean multithread = true;
      String secret = NetAdapterConstants.DEFAULT_SECRET;
      boolean multicast = true;
      int mcPort = NetAdapterConstants.DEFAULT_MULTICAST_PORT;
      String mcGroup = NetAdapterConstants.DEFAULT_MULTICAST_GROUP;

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

               test = OneWireAccessProvider.getProperty("NetAdapter.Multithread");
               if(test!=null)
                  multithread = Boolean.valueOf(test).booleanValue();

               test = OneWireAccessProvider.getProperty("NetAdapter.Secret");
               if(test!=null)
                  secret = test;

               test = OneWireAccessProvider.getProperty("NetAdapter.Multicast");
               if(test!=null)
                  multicast = Boolean.valueOf(test).booleanValue();

               test = OneWireAccessProvider.getProperty("NetAdapter.MulticastPort");
               if(test!=null)
                  mcPort = Integer.parseInt(test);

               test = OneWireAccessProvider.getProperty("NetAdapter.MulticastGroup");
               if(test!=null)
                  mcGroup = test;
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
               else if(arg.equalsIgnoreCase("-multithread"))
               {
                  multithread = Boolean.valueOf(args[++i]).booleanValue();
               }
               else if(arg.equalsIgnoreCase("-secret"))
               {
                  secret = args[++i];
               }
               else if(arg.equalsIgnoreCase("-multicast"))
               {
                  multicast = Boolean.valueOf(args[++i]).booleanValue();
               }
               else if(arg.equalsIgnoreCase("-multicastPort"))
               {
                  mcPort = Integer.parseInt(args[++i]);
               }
               else if(arg.equalsIgnoreCase("-multicastGroup"))
               {
                  mcGroup = args[++i];
               }
               else if(!arg.equalsIgnoreCase("-props"))
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

      System.out.println(
         "  Adapter Name: " + adapter.getAdapterName() + "\r\n" +
         "  Adapter Name: " + adapter.getPortName() + "\r\n" +
         "  Host Listen Port: " + listenPort + "\r\n" +
         "  Multithreaded Host: " + (multithread?"Enabled":"Disabled") + "\r\n" +
         "  Shared Secret: '" + secret + "'\r\n" +
         "  Multicast: " + (multicast?"Enabled":"Disabled") + "\r\n" +
         "  Multicast Port: " + mcPort + "\r\n" +
         "  Multicast Group: " + mcGroup + "\r\n");

      // Create the NetAdapterHost
      NetAdapterHost host = new NetAdapterHost(adapter, listenPort, multithread);
      // set the shared secret
      host.setSecret(secret);

      if(multicast)
      {
         System.out.println("Starting Multicast Listener");
         host.createMulticastListener(mcPort, mcGroup);
      }

      System.out.println("Starting NetAdapter Host");
      (new Thread(host)).start();
   }
}