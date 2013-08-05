package at.mkwe.jvicp.lecroy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.UnknownHostException;

import at.mkwe.jvicp.JVICP;
import at.mkwe.jvicp.JVICP.VICPData;

public class TestConsole {

  public static void main(String[] args) {
    JVICP vicp = new JVICP();
    vicp.setTimeoutDisabled(false);
    if (args.length != 1) {
      System.out.println("USAGE: TestConsole <oscilloscope-IP>");
    } else {
      try {
        vicp.connect(args[0]);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String line = null;
        while ((line = reader.readLine()) != null && !line.equals("quit")) {
          if (line.equals("")) {
            continue;
          }
          if (line.equals("reconnect")) {
            vicp.disconnect();
            vicp.connect(args[0]);
            System.out.println("--- Reconnected!");
            continue;
          }
          vicp.sendDataToDevice(line, true);
          VICPData data = vicp.readDataFromDevice();
          if (data != null)
            System.out.println(data);
        }
      } catch (UnknownHostException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

}
