package at.mkwe.jvicp.lecroy;

import java.io.IOException;
import java.net.UnknownHostException;

import at.mkwe.jvicp.JVICP;
import at.mkwe.jvicp.JVICP.VICPData;

public class LecroyTest {

  public static void main(String[] args) {
    JVICP vicp = new JVICP();
    try {
      vicp.connect("192.168.0.5");
      vicp.sendDataToDevice("*IDN?", true);
      VICPData data = vicp.readDataFromDevice();
      System.out.println(data);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
