/***********************************************************************************
 * AUTHORS: 
 *    Booz Allen Hamilton
 *    Tobin Kaneshiro
 *    Richard Coleman
 * 
 * OBJECTIVE:
 *    This program is intended as a protocol bridge (USB to RS-422) to allow 
 *    interactions between the RCP and DMR computers for shipboard phone management. 
 *    Additionally, this implementation provides a message trouble-shooting 
 *    capability, and a migration path to future protocols, such as Ethernet.
 **********************************************************************************/


#include <SPI.h>         // needed for Arduino versions later than 0018
#include <Ethernet.h>
#include <EthernetUdp.h>         // UDP library from: bjoern@cs.stanford.edu 12/30/2008

// define structures and enumerations
enum SourceEnum {
  RCP,
  DMR
};

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:
byte mac[] = {0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED};
IPAddress ip(192, 168, 1, 177);

//remote address
IPAddress remote_addr(192, 168, 1, 255);
unsigned int localPort = 8888;      // local port to listen on
unsigned int remote_port = 54321;

// Global variables
unsigned long lastByteDMR;
unsigned long lastByteRCP;

// buffers for receiving and sending data
const int SERIAL_BUFFER_MAX_SIZE = 128;
bool rcpStarted;
int rcpOffset;
byte rcpMessage[SERIAL_BUFFER_MAX_SIZE];

bool dmrStarted;
int dmrOffset;
byte dmrMessage[SERIAL_BUFFER_MAX_SIZE];

// An EthernetUDP instance to let us send and receive packets over UDP
EthernetUDP Udp;

const int ENABLE_PIN = 5;

/*
 * Required method by Arduino to initialize the computer during power up...
 */
void setup() {
  resetSerialBuffer(RCP);
  resetSerialBuffer(DMR);

  Serial.begin(19200);
  Serial1.begin(19200);
  
  // start the Ethernet and UDP:
  Ethernet.begin(mac, ip);
  Udp.begin(localPort);

  // initialize timestamps for traffic telemetry...
  lastByteDMR = millis();
  lastByteRCP = millis();

  pinMode(ENABLE_PIN, OUTPUT);
  digitalWrite(ENABLE_PIN, LOW);
}

/*
 * Required method that will be call continuously as the main processing loop.
 */
void loop() {
  delay(25);
}

/*
 * This is the serial port event handler to for serial data interrupts.
 */
void serialEvent() {
  while (Serial.available()) {
    byte data = Serial.read();

    if (!rcpStarted) {
      if (data != '@') continue;
      rcpStarted = true;
    }
    
    rcpMessage[rcpOffset++] = data;
    SendOneByte(data, RCP);

    if (data == '%') {
      sendSerialMessage();
      sendMsgTelemetry(RCP);
    }
  }
}

/*
 * This is the serial port event handler to for serial data interrupts.
 */
void serialEvent1() {
  while (Serial1.available()) {
    byte data = (char)Serial1.read();

    if (!dmrStarted) {
      if (data != '@') continue;
      dmrStarted = true;
    }
    
    dmrMessage[dmrOffset++] = data;
    Serial.write(data);
    SendOneByte(data, DMR);

    if (data == '%') {
      sendMsgTelemetry(DMR);
    }
  }
}

void resetSerialBuffer(SourceEnum source) {
  byte *msg;
  int  *offset;
  bool *started;
  char dirData;
  
  switch (source) {
    case RCP: {
      msg = rcpMessage;
      offset = &rcpOffset;
      started = &rcpStarted;
      dirData = (char)0x06;
      //dirData = 'R';
      break;
    }

    case DMR: {
      msg = dmrMessage;
      offset = &dmrOffset;
      started = &dmrStarted;
      dirData = (char)0x07;
      //dirData = 'D';
      break;
    }
  }
  
  *offset = 0;

  // initial byte denotes direction on the UDP Monitor
  msg[(*offset)++] = dirData;
  *started = false;
}

void SendOneByte(byte data, SourceEnum source) {
  byte diagnostics[6];
  unsigned long timestamp = millis();
  unsigned long diff;
  int bytesSent = 0;
  
  switch (source) {
    case RCP: {
      diagnostics[0] = 0;
      diff = timestamp - lastByteRCP;
      lastByteRCP = timestamp;
      break;
    }

    case DMR: {
      diagnostics[0] = 1;
      diff = timestamp - lastByteDMR;
      lastByteDMR = timestamp;
      break;
    }

    default: {
      return;
    }
  }

  // set the 'Data' field of the message...
  diagnostics[1] = data;

  // set the 'Interval' field in (ms) since last byte read: Big Endian format...
  diagnostics[2] = (diff & 0xFF000000) >> 24;
  diagnostics[3] = (diff & 0x00FF0000) >> 16;
  diagnostics[4] = (diff & 0x0000FF00) >> 8;
  diagnostics[5] = (diff & 0x000000FF);

  // send a reply to the IP address and port that sent us the packet we received
  Udp.beginPacket(remote_addr, remote_port);
  bytesSent = Udp.write(diagnostics, 6);
  Udp.endPacket();
}

void sendMsgTelemetry(SourceEnum source)
{
  byte *msg;
  int  *offset;
  bool *started;
  char dirData;
  int bytesSent = 0;
  
  switch (source) {
    case RCP: {
      msg = rcpMessage;
      offset = &rcpOffset;
      started = &rcpStarted;
      dirData = (char)0x06;
      break;
    }

    case DMR: {
      msg = dmrMessage;
      offset = &dmrOffset;
      started = &dmrStarted;
      dirData = (char)0x07;
      break;
    }

    default: {
      return;
    }
  }

  // send a reply to the IP address and port that sent us the packet we received
  Udp.beginPacket(remote_addr, remote_port);
  bytesSent = Udp.write(msg, *offset);
  Udp.endPacket();

  // reset the RCP message buffer related variables
  rcpOffset = 0;

  // initial byte denotes direction on the UDP Monitor
  msg[(*offset)++] = dirData;
  *started = false;
}

/***********************************************************************************
 * PURPOSE: This function will send the buffered RCP message to the DMR computer. 
 * This buffering is required because the RS-422 interface is setup as a bus, instead
 * of point-to-point; therefore the the Enable pin is used to activate the RS-422 
 * transmission channel, before sending each byte of the RCP message in sequence. 
 * Once the transmission is complete, then RS-422 will disable the RS-422 transmission 
 * channel again, and return to a read-only state. Since serial bytes received from 
 * the DMR computer are always ready to read, no buffering is required for this 
 * direction of data transfer.
 **********************************************************************************/
void sendSerialMessage()
{
  // enable the RS-422 transmit, during the RCP message transmission.
  digitalWrite(ENABLE_PIN, HIGH);
    // is a delay required here?
  
  int i; // skip the first byte that contains telemetry data...
  for (i=1; i<rcpOffset; i++) {
    Serial1.write(rcpMessage[i]);
  }

    // is a delay required here?
  digitalWrite(ENABLE_PIN, LOW);
}


