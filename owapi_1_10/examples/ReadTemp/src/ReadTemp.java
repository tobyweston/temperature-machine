
/*---------------------------------------------------------------------------
 * Copyright (C) 1999,2000 Dallas Semiconductor Corporation, All Rights Reserved.
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

import com.dalsemi.onewire.*;
import com.dalsemi.onewire.adapter.*;
import com.dalsemi.onewire.container.*;

import java.util.Vector;
import java.io.*;

import com.dalsemi.onewire.utils.CRC16;

import java.util.*;


public class ReadTemp {
  static int parseInt(BufferedReader in, int def) {
    try {
      return Integer.parseInt(in.readLine());
    } catch (Exception e) {
      return def;
    }
  }

  /**
   * Method printUsageString
   */
  public static void printUsageString() {
    System.out.println("Temperature Container Demo\r\n");
    System.out.println("Usage: ");
    System.out.println("   java ReadTemp ADAPTER_PORT\r\n");
    System.out.println("ADAPTER_PORT is a String that contains the name of the");
    System.out.println("adapter you would like to use and the port you would like");
    System.out.println("to use, for example: ");
    System.out.println("   java ReadTemp {DS1410E}_LPT1\r\n");
    System.out.println("You can leave ADAPTER_PORT blank to use the default one-wire adapter and port.");
  }

  /**
   * Method main
   *
   * @param args
   * @throws OneWireException
   * @throws OneWireIOException
   */
  public static void main(String[] args) throws OneWireIOException, OneWireException {
    boolean usedefault = false;
    DSPortAdapter access = null;
    String adapter_name = null;
    String port_name = null;

    if ((args == null) || (args.length < 1)) {
      try {
        access = OneWireAccessProvider.getDefaultAdapter();

        if (access == null)
          throw new Exception();
      } catch (Exception e) {
        System.out.println("Couldn't get default adapter!");
        printUsageString();

        return;
      }

      usedefault = true;
    }

    if (!usedefault) {
      StringTokenizer st = new StringTokenizer(args[0], "_");

      if (st.countTokens() != 2) {
        printUsageString();

        return;
      }

      adapter_name = st.nextToken();
      port_name = st.nextToken();

      System.out.println("Adapter Name: " + adapter_name);
      System.out.println("Port Name: " + port_name);
    }

    if (access == null) {
      try {
        access = OneWireAccessProvider.getAdapter(adapter_name, port_name);
      } catch (Exception e) {
        System.out.println("That is not a valid adapter/port combination.");

        Enumeration en = OneWireAccessProvider.enumerateAllAdapters();

        while (en.hasMoreElements()) {
          DSPortAdapter temp = (DSPortAdapter) en.nextElement();

          System.out.println("Adapter: " + temp.getAdapterName());

          Enumeration f = temp.getPortNames();

          while (f.hasMoreElements()) {
            System.out.println("   Port name : " + ((String) f.nextElement()));
          }
        }

        return;
      }
    }

    access.adapterDetected();
    access.targetAllFamilies();
    access.beginExclusive(true);
    access.reset();
    access.setSearchAllDevices();

    boolean next = access.findFirstDevice();

    if (!next) {
      System.out.println("Could not find any iButtons!");

      return;
    }

    while (next) {
      OneWireContainer owc = access.getDeviceContainer();

      System.out.println("====================================================");
      System.out.println("= Found One Wire Device: " + owc.getAddressAsString() + "          =");
      System.out.println("====================================================");
      System.out.println("=");

      boolean isTempContainer = false;
      TemperatureContainer tc = null;

      try {
        tc = (TemperatureContainer) owc;
        isTempContainer = true;
      } catch (Exception e) {
        tc = null;
        isTempContainer = false;   //just to reiterate
      }

      if (isTempContainer) {
        System.out.println("= This device is a " + owc.getName());
        System.out.println("= Also known as a " + owc.getAlternateNames());
        System.out.println("=");
        System.out.println("= It is a Temperature Container");

        double max = tc.getMaxTemperature();
        double min = tc.getMinTemperature();
        boolean hasAlarms = tc.hasTemperatureAlarms();

        System.out.println("= This device " + (hasAlarms ? "has" : "does not have") + " alarms");
        System.out.println("= Maximum temperature: " + max);
        System.out.println("= Minimum temperature: " + min);

        double high = 0.0;
        double low = 0.0;
        byte[] state = tc.readDevice();

        if (hasAlarms) {
          high = tc.getTemperatureAlarm(tc.ALARM_HIGH, state);
          low = tc.getTemperatureAlarm(tc.ALARM_LOW, state);

          System.out.println("= High temperature alarm set to : " + high);
          System.out.println("= Low temperature alarm set to  : " + low);
        }

        double resol = 0.0;
        boolean selectable = tc.hasSelectableTemperatureResolution();

        if (hasAlarms) {
          resol = tc.getTemperatureAlarmResolution();

          System.out.println("= Temperature alarm resolution  : " + resol);
        }

        double tempres = tc.getTemperatureResolution(state);
        double[] resolution = null;

        System.out.println("= Temperature resolution        : " + tempres);
        System.out.println("= Resolution is selectable      : " + selectable);

        if (selectable)
          try {
            resolution = tc.getTemperatureResolutions();

            for (int i = 0; i < resolution.length; i++)
              System.out.println("= Available resolution " + i + "        : " + resolution[i]);
          } catch (Exception e) {
            System.out.println("= Could not get available resolutions : " + e.toString());
          }

        if (hasAlarms) {
          System.out.println("= Setting high temperature alarm to 28.0 C...");
          tc.setTemperatureAlarm(tc.ALARM_HIGH, 28.0, state);
          System.out.println("= Setting low temperature alarm to 23.0 C...");
          tc.setTemperatureAlarm(tc.ALARM_LOW, 23.0, state);
        }

        if (selectable)
          try {
            System.out.println("= Setting temperature resolution to " + resolution[0] + "...");
            tc.setTemperatureResolution(resolution[0], state);
          } catch (Exception e) {
            System.out.println("= Could not set resolution: " + e.toString());
          }

        try {
          tc.writeDevice(state);
          System.out.println("= Device state written.");
        } catch (Exception e) {
          System.out.println(
              "= Could not write device state, all changes lost.");
          System.out.println("= Exception occurred: " + e.toString());
        }

        System.out.println("= Doing temperature conversion...");

        try {
          tc.doTemperatureConvert(state);
        } catch (Exception e) {
          System.out.println(
              "= Could not complete temperature conversion...");
        }

        state = tc.readDevice();

        if (hasAlarms) {
          high = tc.getTemperatureAlarm(tc.ALARM_HIGH, state);
          low = tc.getTemperatureAlarm(tc.ALARM_LOW, state);

          System.out.println("= High temperature alarm set to : "
              + high);
          System.out.println("= Low temperature alarm set to  : " + low);
        }

        double temp = tc.getTemperature(state);

        System.out.println("= Reported temperature: " + temp);
      } else {
        System.out.println("= This device is not a temperature device.");
        System.out.println("=");
        System.out.println("=");
      }

      next = access.findNextDevice();
    }
  }
}
