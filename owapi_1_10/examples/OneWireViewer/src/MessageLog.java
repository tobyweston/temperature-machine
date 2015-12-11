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

/**
 * Represents the interface for all message loggers.  The OneWireViewer frame,
 * as well as each individual Viewer, will log all errors to an instance of
 * this interface.  Each error must be one of the 4 types:
 * <ul>
 *    <li>Error Messages - <code>MessageLog.ERRMSG</code></li>
 *    <li>Warning Messages - <code>MessageLog.WARNMSG</code></li>
 *    <li>Informational Messages - <code>MessageLog.MESSAGE</code></li>
 *    <li>Verbose Messages - <code>MessageLog.VERBOSE</code></li>
 * </ul>
 *
 * The list of message types goes from the lowest-order to the highest-order.
 * All higher-order message types subsume lower-level messages types when it
 * comes to the <code>setLevel(int type);</code> method.  Meaning, if you
 * set the level to MessageLog.VERBOSE, all message types will be logged.
 * If you set the level to MessageLog.WARNMSG, only Warnings and Errors will
 * be logged.
 *
 * The message log interface can be used to show messages in a windowed
 * environment, log messages to an ODBC database, or write messages to
 * a file.
 *
 * @author SH
 */
public interface MessageLog
{
   /* Message types */
   /** None */
   public static final int NONE    = -1;
   /** Error Message */
   public static final int ERRMSG  = 0;
   /** Warning  Message */
   public static final int WARNMSG = 1;
   /** Informational Message */
   public static final int MESSAGE = 2;
   /** Verbose Message */
   public static final int VERBOSE = 3;

   /** Labels for each message type entry */
   public static final String[] LABELS = { "ERROR", "WARNING",
                                           "MESSAGE", "VERBOSE" };

   /**
    * Adds an Error Message to the log.
    *
    * @param viewerName Viewer that was the source of the error.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the error
    *              was generated.
    * @param error Actual message which explains the error.
    */
   public void addError(String viewerName, String romID, String error);

   /**
    * Adds an Warning Message to the log.
    *
    * @param viewerName Viewer that was the source of the warning.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the warning
    *              was generated.
    * @param warning Actual message which explains the warning.
    */
   public void addWarning(String viewerName, String romID, String warning);

   /**
    * Adds an Informational Message to the log.
    *
    * @param viewerName Viewer that was the source of the message.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the message
    *              was generated.
    * @param message Actual message.
    */
   public void addMessage(String viewerName, String romID, String message);

   /**
    * Adds an Error Message to the log.
    *
    * @param viewerName Viewer that was the source of the verbose message.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the verbose
    *              message was generated.
    * @param verbose Actual message.
    */
   public void addVerbose(String viewerName, String romID, String verbose);

   /**
    * Adds a message to the log.
    *
    * @param type The message type.  Either <code>MessageLog.ERRMSG</code>,
    *             <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    *             or <code>MessageLog.VERBOSE</code>.
    * @param viewerName Viewer that was the source of the error.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the error
    *              was generated.
    * @param entry Actual message.
    */
   public void addEntry(int type, String viewerName, String romID, String entry);

   /**
    * Sets the level of logging.  Either <code>MessageLog.ERRMSG</code>,
    * <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    * or <code>MessageLog.VERBOSE</code>.  Setting the level to
    * <code>MessageLog.VERBOSE</code> causes all messages to be displayed.  While
    * Setting the level to <code>MessageLog.WARNMSG</code> causes only warnings
    * and errors to be displayed.
    *
    * @param level The message types for logging.  Either <code>MessageLog.ERRMSG</code>,
    *             <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    *             or <code>MessageLog.VERBOSE</code>.
    */
   public void setLevel(int level);

   /**
    * Gets the level of logging.  Either <code>MessageLog.ERRMSG</code>,
    * <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    * or <code>MessageLog.VERBOSE</code>.
    *
    * @param level The message types that are logge.  Either <code>MessageLog.ERRMSG</code>,
    *             <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    *             or <code>MessageLog.VERBOSE</code>.
    */
   public int getLevel();

}
