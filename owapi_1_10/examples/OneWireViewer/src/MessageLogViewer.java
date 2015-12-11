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

import javax.swing.*;
import java.awt.*;

/**
 * A message repository which displays messages in a JPanel.  The OneWireViewer
 * frame, as well as each individual Viewer, will log all errors to an instance
 * of this class.  Each error must be one of the 4 types:
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
 * @author SH
 */
public class MessageLogViewer extends JPanel
   implements MessageLog
{
   int level;
   JTextArea textArea;

   /**
    * Creates a default MessageLogViewer which displays error messages,
    * warnings, and informational messages.
    */
   public MessageLogViewer()
   {
      this(MESSAGE);
   }

   /**
    * Creates a MessageLogViewer which displays the specified message level.
    * Note that higher-order message levels subsume the lower-levels.  So
    * MessageLog.VERBOSE shows all message types, but MessageLog.WARNMSG only
    * shows warnings and errors.
    */
   public MessageLogViewer(int messageLevel)
   {
      super(new BorderLayout());

      textArea = new JTextArea();
      add(new JScrollPane(textArea), BorderLayout.CENTER);

      setLevel(messageLevel);
   }

   /**
    * Clears all currently displayed messages from the log.
    */
   public void clearAll()
   {
      this.textArea.setText("");
   }

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
   public void setLevel(int level)
   {
      if(level<MessageLog.NONE)
         this.level = MessageLog.NONE;
      else if(level>MessageLog.VERBOSE)
         this.level = MessageLog.VERBOSE;
      else
         this.level = level;
   }

   /**
    * Gets the level of logging.  Either <code>MessageLog.ERRMSG</code>,
    * <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    * or <code>MessageLog.VERBOSE</code>.
    *
    * @param level The message types that are logge.  Either <code>MessageLog.ERRMSG</code>,
    *             <code>MessageLog.WARNMSG</code>, <code>MessageLog.MESSAGE</code>,
    *             or <code>MessageLog.VERBOSE</code>.
    */
   public int getLevel()
   {
      return this.level;
   }

   /**
    * Adds an Error Message to the log.
    *
    * @param viewerName Viewer that was the source of the verbose message.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the verbose
    *              message was generated.
    * @param verbose Actual message.
    */
   public void addVerbose(String viewerName, String romID, String verbose)
   {
      addEntry(VERBOSE, viewerName, romID, verbose);
   }

   /**
    * Adds an Informational Message to the log.
    *
    * @param viewerName Viewer that was the source of the message.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the message
    *              was generated.
    * @param message Actual message.
    */
   public void addMessage(String viewerName, String romID, String message)
   {
      addEntry(MESSAGE, viewerName, romID, message);
   }

   /**
    * Adds an Warning Message to the log.
    *
    * @param viewerName Viewer that was the source of the warning.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the warning
    *              was generated.
    * @param warning Actual message which explains the warning.
    */
   public void addWarning(String viewerName, String romID, String warning)
   {
      addEntry(WARNMSG, viewerName, romID, warning);
   }

   /**
    * Adds an Error Message to the log.
    *
    * @param viewerName Viewer that was the source of the error.
    * @param romID 64-bit RomID of the 1-Wire part that was in use when the error
    *              was generated.
    * @param error Actual message which explains the error.
    */
   public void addError(String viewerName, String romID, String error)
   {
      addEntry(ERRMSG, viewerName, romID, error);
   }

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
   public void addEntry(int type, String viewerName, String romID, String error)
   {
      if(this.level>=type)
      {
         textArea.append(LABELS[type] + ": " + viewerName + " (" + romID
                      + ") " + error + "\r\n");
      }
   }
}
