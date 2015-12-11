/*---------------------------------------------------------------------------
 * Copyright (C) 2001 Dallas Semiconductor Corporation, All Rights Reserved.
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

/**
 * ViewerProperties - a static class for proxying calls to a <code>Properties</code>
 * object.  ViewerProperties ensures that properties are loaded to and saved from
 * a centralized location.  The properties file will attempt to load from the
 * current directory first.  If that load fails, the <java home>/lib directory
 * is used.  If that load files, a new file is created in the home directory for
 * storing viewer properties.
 *
 * @author SKH
 * @version 1.00
 */
public class ViewerProperties
{
   /** Properties file that this class proxies to */
   private static Properties props = null;

   /** filename of the properties file on disk */
   private static final String filename = "onewireviewer.properties";

   /** of the paths to check, which one was used */
   private static int pathUsed = -1;

   /** a list of paths to check for properties file */
   private static final String[] pathsToCheck = new String[]
      {
         System.getProperty("java.home") + File.separator + "lib"
            + File.separator, //the java lib directory
         "" //the current directory
      };

   /** used to convert hex strings to integer values */
   private static final char[] lookup = new char[]
   {
      '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
   };

   /** don't instantiate this class */
   private ViewerProperties() {;}

   /**
    * Gets the specfied onewire property.
    * Looks for the property in the following locations:
    * <p>
    * <ul>
    * <li> In System.properties
    * <li> In onewireviewer.properties file in current directory
    *      or < java.home >/lib/ (Desktop) or /etc/ (TINI)
    * </ul>
    *
    * @param propName string name of the property to read
    *
    * @return  <code>String</code> representing the property value or <code>null</code> if
    *          it could not be found
    */
   public static String getProperty (String propName)
   {
      // first, try system properties
      try
      {
         String ret_str = System.getProperty(propName, null);
         if(ret_str!=null)
            return ret_str;
      }
      catch (Exception e)
      { ; }

      // if system property is not set try onewireviewer.properties file
      synchronized(pathsToCheck)
      {
         if(props==null)
            loadProperties();
      }

      Object ret = null;
      if(props!=null)
         ret = props.get(propName);
      return (String)ret;
   }

   /**
    * Gets the specfied onewire property.
    *
    * @param propName string name of the property to read
    * @param defValue Value to return if property is not defined
    *
    * @return  <code>String</code> representing the property value or
    *          <code>defValue</code> if it could not be found
    */
   public static String getProperty (String propName, String defValue)
   {
      String ret = getProperty(propName);
      return (ret==null) ? defValue : ret;
   }

   /**
    * Gets the specfied onewire property.
    *
    * @param propName string name of the property to read
    * @param defValue Value to return if property is not defined
    *
    * @return  <code>boolean</code> representing the property value or
    *          <code>defValue</code> if it could not be found
    */
   public static boolean getPropertyBoolean(String propName, boolean defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
         defValue = Boolean.valueOf(strValue).booleanValue();
      return defValue;
   }

   /**
    * Gets the specfied onewire property.
    *
    * @param propName string name of the property to read
    * @param defValue Value to return if property is not defined
    *
    * @return  <code>int</code> representing the property value or
    *          <code>defValue</code> if it could not be found
    */
   public static int getPropertyInt(String propName, int defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
         defValue = Integer.parseInt(strValue);
      return defValue;
   }

   /**
    * Gets the specfied onewire property.  Derives integer from a hex
    * numeric string.
    *
    * @param propName string name of the property to read
    * @param defValue Value to return if property is not defined
    *
    * @return  <code>int</code> representing the property value or
    *          <code>defValue</code> if it could not be found
    */
   public static int getPropertyHexInt(String propName, int defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
         defValue = Integer.parseInt(strValue,16);
      return defValue;
   }

   /**
    * Gets the specfied onewire property.  Supports a maximum of 128 bytes.
    *
    * @param propName string name of the property to read
    * @param defValue Value to return if property is not defined
    *
    * @return  <code>byte[]</code> representing the property value or
    *          <code>defValue</code> if it could not be found
    */
   public static byte[] getPropertyBytes(String propName, byte[] defValue)
   {
      String strValue = getProperty(propName);
      if(strValue!=null)
      {
         //only supports up to 128 bytes of data
         byte[] tmp = new byte[128];

         //split the string on commas and spaces
         StringTokenizer strtok = new StringTokenizer(strValue,", ");

         //how many bytes we got
         int i = 0;
         while(strtok.hasMoreElements())
         {
            //this string could have more than one byte in it
            String multiByteStr = strtok.nextToken();
            int strLen = multiByteStr.length();

            for(int j=0; j<strLen; j+=2)
            {
               //get just two nibbles at a time
               String byteStr
                  = multiByteStr.substring(j, Math.min(j+2, strLen));

               long lng = 0;
               try
               {
                  //parse the two nibbles into a byte
                  lng = Long.parseLong(byteStr, 16);
               }
               catch(NumberFormatException nfe)
               {
                  nfe.printStackTrace();

                  //no mercy!
                  return defValue;
               }

               //store the byte and increment the counter
               if(i<tmp.length)
                  tmp[i++] = (byte)(lng&0x0FF);
            }
         }
         if(i>0)
         {
            byte[] retVal = new byte[i];
            System.arraycopy(tmp, 0, retVal, 0, i);
            return retVal;
         }
      }
      return defValue;
   }

   /**
    * Sets the specfied onewire property.
    *
    * @param propName string name of the property
    * @param value Value of the property to set
    */
   public static void setProperty(String propName, String value)
   {
      synchronized(pathsToCheck)
      {
         if(props==null)
         {
            if(!loadProperties())
               props = new Properties();
         }
      }
      props.put(propName, value);
   }

   /**
    * Sets the specfied onewire property.
    *
    * @param propName string name of the property
    * @param value Value of the property to set
    */
   public static void setPropertyBoolean(String propName, boolean value)
   {
      setProperty(propName, value+"");
   }

   /**
    * Sets the specfied onewire property.  Formats integer as a hex
    * numeric string.
    *
    * @param propName string name of the property
    * @param value Value of the property to set
    */
   public static void setPropertyHexInt(String propName, int value)
   {

      setProperty(propName, Long.toHexString(value));
   }

   /**
    * Sets the specfied onewire property.
    *
    * @param propName string name of the property
    * @param value Value of the property to set
    */
   public static void setPropertyInt(String propName, int value)
   {
      setProperty(propName, value+"");
   }

   /**
    * Sets the specfied onewire property.
    *
    * @param propName string name of the property
    * @param value Value of the property to set
    */
   public static void setPropertyBytes(String propName, byte[] bytes)
   {
      StringBuffer sb = new StringBuffer(bytes.length*2);
      for(int i=0; i<bytes.length; i++)
      {
         sb.append(lookup[(bytes[0]&0xF0)>>4]);
         sb.append(lookup[bytes[0]&0x0F]);
      }
      setProperty(propName, sb.toString());
   }


   /**
    * Attempts to load the value of all properties from disk.  First checks
    * current directory for properties file, then the system lib directory.
    *
    * @reaturn <code>true</code> if load was successful.
    */
   public static boolean loadProperties()
   {
      int i=0;
      for(; props==null && i<pathsToCheck.length; i++)
      {
         // attempt to open the properties file in two locations
         FileInputStream prop_file = null;
         try
         {
            // attempt to open the file
            prop_file = new FileInputStream(pathsToCheck[i]+filename);
            // attempt to read the onewire.properties
            props = new Properties();
            props.load(prop_file);
            pathUsed = i;
            return true;
         }
         catch (Exception e)
         {
            //System.err.println("Couldn't find properties file in " + pathsToCheck[i]);
            props = null;
         }
      }

      return false;
   }

   public static String getPropertiesFilename()
   {
      // if we didn't find a file, create one in <java home>/lib folder
      if(pathUsed<0)
         pathUsed = 0;

      return pathsToCheck[pathUsed] + filename;
   }

   /**
    * Attempts to save the value of all properties to disk.  If properties
    * file was loaded from disk, it is saved in the same directory.  Otherwise,
    * a new file is created in the current directory.
    *
    * @reaturn <code>true</code> if load was successful.
    */
   public static boolean saveProperties()
   {
      try
      {
         FileOutputStream fos = new FileOutputStream(getPropertiesFilename());
         props.store(fos,"OneWireViewer Properties");
         fos.close();

         return true;
      }
      catch(Exception e)
      {
         System.err.println("Couldn't save properties file! "+e);
         e.printStackTrace();
      }
      return false;
   }
}
