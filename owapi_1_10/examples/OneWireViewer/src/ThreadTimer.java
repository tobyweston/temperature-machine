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

import java.awt.event.*;

/**
 * An extension of Thread designed to fire events with a certain delay in
 * between each thread.  The only gaurantee this class makes is that the
 * time between firing an action event will be <u>at least</u> as long as
 * the delay specified.  The ThreadTimer also has a minimum resolution of
 * 100 milliseconds.  Meaning, no delay less than 100 ms is possible and
 * all delays should be multiples of 100 ms.  If you specify a delay of
 * 150 ms, at least 200 ms will pass before ThreadTimer fires an event.
 *
 * @author SKH
 * @version 1.0
 */
public class ThreadTimer extends Thread
{
   /** The Default delay if none is specified */
   public static final int DEFAULT_DELAY = 1000;

   /** The minimum delay allowed.  Also specifies the resolution of the timer. */
   public static final int MIN_DELAY = 100;

   /** constant action event, used when calling actionPerformed of listeners */
   private final ActionEvent ae
      = new ActionEvent(this, -1, "ThreadTimer Fired");

   /** unique ID source, to identify each thread (for debugging) */
   private static int ID = 0;

   /** protect access to unique ID source from multiple threads */
   private static Object syncFlag = new Object();

   /** particular unique ID for this thread */
   private int id;

   /** the amount of time to delay between firing events */
   private volatile int msDelay;

   /** boolean values for pausing, stopping, interrupting, and polling the thread */
   private volatile boolean paused = false,
                            stopped = false,
                            interrupted = false,
                            isRunning = false;

   /** a list of ActionListeners who will receive events from this thread */
   private ActionListener[] listeners;

   /** MessageLog, for logging messages */
   private MessageLog log = null;

   /**
    * Creates a new ThreadTimer using the <code>DEFAULT_DELAY</code> and
    * firing events to the specified ActionListener.
    *
    * @param al listener who will receive ActionEvents from this ThreadTimer.
    */
   public ThreadTimer(ActionListener al, MessageLog log)
   {
      this(DEFAULT_DELAY, al, log);
   }

   /**
    * Creates a new ThreadTimer using the specified delay and
    * firing events to the specified ActionListener.
    *
    * @param delay the amount of time (at least) to put between events.
    * @param al listener who will receive ActionEvents from this ThreadTimer.
    */
   public ThreadTimer(int delay, ActionListener al, MessageLog log)
   {
      setDelay(delay);
      this.listeners = new ActionListener[] { al };
      this.log = log;

      synchronized(syncFlag)
      {
         id = ID++;
      }
   }

   /**
    * Creates a new ThreadTimer using the specified delay.  ActionListeners
    * will still have to be specified using the <code>addActionListener</code>
    * method.
    *
    * @param delay the amount of time (at least) to put between events.
    */
   public ThreadTimer(int delay, MessageLog log)
   {
      setDelay(delay);
      this.listeners = new ActionListener[0];
      this.log = log;

      synchronized(syncFlag)
      {
         id = ID++;
      }
   }

   /**
    * Run method of this thread.  While it hasn't been stopped and it isn't
    * paused, this method continuously delays and fires events.
    */
   public void run()
   {
      isRunning = true;
      while(!stopped)
      {
         while(!paused)
         {
            fire();
            delay();
         }
         if(!stopped)
            delay();
      }
      isRunning = false;
   }

   /**
    * Fires an event to all actionlisteners.  Public so that other threads
    * can force an immediate fire and allow operations to execute in their
    * context.
    */
   public synchronized void fire()
   {
      try
      {
         for(int i=0; i<this.listeners.length; i++)
            this.listeners[i].actionPerformed(ae);
      }
      catch(Exception e)
      {
         log.addError("ThreadTimer", "N/A", e.getMessage());
      }
   }

   /**
    * Pauses the ThreadTimer from executing.
    */
   public void pauseTimer()
   {
      this.paused = true;
   }

   /**
    * Forces the ThreadTimer to resume, if not stopped.
    */
   public void resumeTimer()
   {
      this.paused = false;
   }

   /**
    * Stops the ThreadTimer and optionally waits for it to complete
    * all tasks.
    *
    * @param waitForThread If <code>true</code>, this method blocks until
    *                      the thread is dead.
    */
   public void killTimer(boolean waitForThread)
   {
      this.interrupted = true;
      this.paused = true;
      this.stopped = true;

      while(waitForThread && isRunning)
      {
         yield();
         try
         {
            sleep(MIN_DELAY);
         }
         catch(InterruptedException ie)
         {;}
      }
   }

   /**
    * Sets the delay between action events.
    *
    * @param delay the amount of time (in ms) to wait between action events.
    */
   public void setDelay(int delay)
   {
      if(delay<MIN_DELAY)
         delay = DEFAULT_DELAY;
      this.msDelay = delay;
   }

   /**
    * Gets the delay between action events.
    *
    * @return the amount of time (in ms) to wait between action events.
    */
   public int getDelay()
   {
      return this.msDelay;
   }

   /**
    * Adds an ActionListener to the list of ActionListeners which will
    * be notified when this thread fires an ActionEvent.
    *
    * @param al listener who will receive ActionEvents from this ThreadTimer.
    */
   public synchronized void addActionListener(ActionListener al)
   {
      int cnt = this.listeners.length;
      ActionListener[] temp = new ActionListener[cnt+1];
      temp[cnt] = al;
      System.arraycopy(this.listeners, 0, temp, 0, cnt);
      this.listeners = temp;
   }

   /**
    * Interrupts the thread if it is currently in a delay loop.  Used to cause
    * an (almost) immediate firing of events in this thread's context.
    */
   public void interruptDelay()
   {
      this.interrupted = true;
   }

   /**
    * Delay's the specified amount of time.
    */
   private void delay()
   {
      int delay = msDelay;
      while(!this.interrupted && (delay-=MIN_DELAY)>0)
      {
         yield();
         try
         {
            sleep(MIN_DELAY);
         }
         catch(InterruptedException ie)
         {;}
      }
      this.interrupted = false;
   }
}