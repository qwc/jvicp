package at.mkwe.jvicp.lecroy;

import java.io.IOException;
import java.net.UnknownHostException;

import at.mkwe.jvicp.JVICP;
import at.mkwe.jvicp.JVICP.VICPData;

public class LecroyTest {

  public static void main(String[] args) {
    JVICP vicp = new JVICP();
    try {
      // connect to lecroy waveace 2002
      vicp.connect("192.168.0.5");
      // send identification query
      vicp.sendDataToDevice("*IDN?", true);
      // read identification
      VICPData data = vicp.readDataFromDevice();
      // fire it out on the console
      System.out.println(data);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
