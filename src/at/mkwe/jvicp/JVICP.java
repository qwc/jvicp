package at.mkwe.jvicp;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/*
 * Ported to Java from "Lightweight VICP client implementation" (C++) by Anathony Cake
 * by Marcel M. Otte, MKW Electronics, http://www.mkwe.at
 * 
 * 
 * Original Header comment:
 */
//------------------------------------------------------------------------------------------
//Summary:             Lightweight VICP client implementation.
//
//Started by:  Anthony Cake
//
//Started:             June 2003
//                           Published on SourceForge under LeCroyVICP project, Sept 2003
//------------------------------------------------------------------------------------------
//
//Description: 
//
//           This file contains a Client-side implementation of the VICP network communications
//           protocol used to control LeCroy Digital Oscilloscopes (DSOs).
//
//           This file is intended to be ultimately as platform independent as possible, but at
//           present has only been compiled & tested under Visual C++ 6.0 on a Windows platform.
//
//VICP Protocol Description/History:
//   
//           The VICP Protocol has been around since 1997/98. It did not change in any way between it's
//           conception, and June 2003, when a previously reserved field in the header was assigned. 
//           This field, found at byte #2, is now used to allow the client-end of a VICP communication
//           to detect 'out of sync' situations, and therefore allows the GPIB 488.2 'Unread Response'
//           mechanism to be emulated. 
//           
//   These extensions to the original protocol did not cause a version number change, and are
//           referred to as version 1a. It was decided not to bump the version number to reduce the
//           impact on clients, many of which are looking for a version number of 1. 
//           Clients and servers detect protocol version 1a by examining the sequence number field, 
//           it should be 0 for version 1 of the protocol (early clients), or non-zero for v1a.
//
//
//VICP Headers:
//
//       Byte        Description             
//       ------------------------------------------------
//            0              Operation       
//            1              Version         1 = version 1
//            2              Sequence Number { 1..255 }, (was unused until June 2003)
//            3              Unused
//            4              Block size, LSB  (not including this header)
//            5              Block size
//            6              Block size
//            7              Block size, MSB
//
//   Operation bits:
//
//Bit             Mnemonic        Purpose
//-----------------------------------------------
//D7              DATA            Data block (D0 indicates with/without EOI)
//D6              REMOTE          Remote Mode
//D5              LOCKOUT         Local Lockout (Lockout front panel)
//D4              CLEAR           Device Clear (if sent with data, clear occurs before block is passed to parser)
//D3              SRQ                     SRQ (Device -> PC only)
//D2              SERIALPOLL  Request a serial poll
//D1              Reserved        Reserved for future expansion
//D0              EOI                     Block terminated in EOI
//
//Known Limitations:
//
//Outstanding Issues
//
//------------------------------------------------------------------------------------------
//

public class JVICP {

  // VICP header 'Operation' bits
  public static final int OPERATION_DATA = 0x80;
  public static final int OPERATION_REMOTE = 0x40;
  public static final int OPERATION_LOCKOUT = 0x20;
  public static final int OPERATION_CLEAR = 0x10;
  public static final int OPERATION_SRQ = 0x08;
  public static final int OPERATION_REQSERIALPOLL = 0x04;
  public static final int OPERATION_EOI = 0x01;

  // Header Version
  public static final int HEADER_VERSION1 = 0x01;

  public static final int VICP_PORT = 1861; // port # registered with IANA for
                                            // lecroy-vicp

  public static class VICPHeader {
    byte operation;
    byte headerVersion;
    byte sequenceNumber;
    int numOfBytes;
    private boolean VICPVersion1aSupported;

    public VICPHeader() {
      operation = 0;
      headerVersion = HEADER_VERSION1;
    }

    public boolean validate() {
      if (((operation & OPERATION_DATA) > 0 && headerVersion == HEADER_VERSION1)) {
        if (sequenceNumber > 0) {
          setVICPVersion1aSupported(true);
        } else {
          setVICPVersion1aSupported(false);
        }
        return true;
      }
      return false;
    }

    public void setOperation(int operation) {
      this.operation |= operation;
    }

    public void setHeaderVersion(int headerVersion) {
      this.headerVersion = (byte) headerVersion;
    }

    public void setSequenceNo(int seqNo) {
      this.sequenceNumber = (byte) seqNo;
    }

    public void setPacketLength(int length) {
      numOfBytes = length;
    }

    public boolean isEOI() {
      if ((operation & OPERATION_EOI) > 0) {
        return true;
      }
      return false;
    }

    public boolean isSRQchanged() {
      if ((operation & OPERATION_SRQ) > 0) {
        return true;
      }
      return false;
    }

    public boolean isVICPVersion1aSupported() {
      return VICPVersion1aSupported;
    }

    public void setVICPVersion1aSupported(boolean vICPVersion1aSupported) {
      VICPVersion1aSupported = vICPVersion1aSupported;
    }
  }

  public static class VICPData {
    byte[] bytes;

    public String toString() {
      return new String(bytes, 0, bytes.length);
    }
  }

  private String host;
  private Socket vicp_socket;
  private DataInputStream in;
  private DataOutputStream out;
  private int lastSequenceNumber, nextSequenceNumber;

  // just init with tcp
  public JVICP() {
    init();
  }

  // / init and connect!
  public JVICP(String host) throws UnknownHostException, IOException {
    this.setHost(host);
    init();
    connect(host);
  }

  public void connect(String host) throws UnknownHostException, IOException {
    this.host = host;
    vicp_socket = new Socket(host, VICP_PORT);
    in = new DataInputStream(vicp_socket.getInputStream());
    out = new DataOutputStream(vicp_socket.getOutputStream());
  }

  private void init() {
    lastSequenceNumber = 1;
    nextSequenceNumber = 1;
  }

  public void disconnect() {
    try {
      vicp_socket.close();
    } catch (IOException e) {
      // ignore!
    }
  }

  public void sendDataToDevice(String message, boolean eoiTermination) throws IOException {
    sendDataToDevice(message, eoiTermination, false, false);
  }

  public void sendDataToDevice(String message, boolean eoiTermination, boolean deviceClear) throws IOException {
    sendDataToDevice(message, eoiTermination, deviceClear, false);
  }

  public void sendDataToDevice(String message, boolean eoiTermination, boolean deviceClear, boolean serialPoll) throws IOException {
    VICPHeader header = new VICPHeader();
    header.setOperation(OPERATION_DATA);
    if (eoiTermination)
      header.setOperation(OPERATION_EOI);
    if (deviceClear)
      header.setOperation(OPERATION_CLEAR);
    if (serialPoll)
      header.setOperation(OPERATION_REQSERIALPOLL);
    // implement remote mode? (not used in original C code)
    header.setHeaderVersion(HEADER_VERSION1);
    header.setSequenceNo(getNextSequenceNumber(header));
    header.setPacketLength(message.length());

    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    DataOutputStream dbout = new DataOutputStream(bout);
    dbout.writeByte(header.operation & 0x0FF);
    dbout.writeByte(header.headerVersion & 0x0FF);
    dbout.writeByte(header.sequenceNumber & 0x0FF);
    dbout.writeByte(0x00);
    dbout.writeByte(header.numOfBytes >> 24 & 0x0FF);
    dbout.writeByte(header.numOfBytes >> 16 & 0x0FF);
    dbout.writeByte(header.numOfBytes >> 8 & 0x0FF);
    dbout.writeByte(header.numOfBytes & 0x0FF);

    dbout.writeBytes(message);
    out.write(bout.toByteArray());
    out.flush();
  }

  public VICPData readDataFromDevice() throws IOException {
    VICPHeader header = readHeaderFromDevice();
    VICPData data = new VICPData();
    data.bytes = new byte[header.numOfBytes];
    in.readFully(data.bytes);
    return data;
  }

  public VICPHeader readHeaderFromDevice() throws IOException {
    VICPHeader header = new VICPHeader();
    int cnt = 0, timeout = 5000;
    while (in.available() < 8 && cnt < timeout) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
      }
      ++cnt;
    }
    // error, no sign of a header
    if (cnt == timeout) {
      System.out.println("Reached timeout!");
      this.disconnect();
      this.connect(host);
      return null;
    }
    header.operation = in.readByte();
    header.headerVersion = in.readByte();
    header.sequenceNumber = in.readByte();
    in.readByte();
    header.numOfBytes = in.readInt();

    if (!header.validate()) {
      // reconnect? out of sync?
      this.disconnect();
      this.connect(host);
      return null;
    }
    lastSequenceNumber = header.sequenceNumber;
    return header;
  }

  public int getNextSequenceNumber(VICPHeader header) {
    lastSequenceNumber = nextSequenceNumber;
    if (header.isEOI()) {
      ++nextSequenceNumber;
      if (nextSequenceNumber >= 256)
        nextSequenceNumber = 1;
    }
    return lastSequenceNumber;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

}
