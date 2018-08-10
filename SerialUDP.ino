#include <Arduino.h>
#include <Ethernet.h>
#include <SPI.h>

// Enter a MAC address and IP address for your controller below.
// The IP address will be dependent on your local network:
byte mac[] = {
  0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED
};
IPAddress ip(192, 168, 123, 177);
unsigned int localPort = 24;
unsigned int count;
unsigned int size;

byte packetBuffer[UDP_TX_PACKET_MAX_SIZE];
byte replyBuffer[UDP_TX_PACKET_MAX_SIZE];

//declare instance of UDP
EthernetUDP Udp;

void setup() {
  //feed in mac & ip address
    Ethernet.begin(mac, ip);
    //open port
    Udp.begin(localPort);
    //set up arduinos (multiple arduinos for debugging purposes) while setting baud rates
    Serial.begin(9600);
    Serial1.begin(9600);
}

void loop() {
  //call on to read UDP and send serial
    readPackets();
   //read serial packet and send UDP
    readSerial();
    delay(100);
}

//read in serial and send UDP
void readSerial() {
    //available bytes avaiable for comaprison for packets
    unsigned int numAvailable;
    if((numAvailable = Serial.available())) {
        //We have data :D
        byte byteRead = Serial.read();
        count++;
        if(count == 2) {
            size = byteRead & 0x1f;
        }
        replyBuffer[count - 1] = byteRead;
        if(size > 0 && count >= size * 2 + 4) {

            for(int i = 0; i < UDP_TX_PACKET_MAX_SIZE; i++) {
                Serial1.write(replyBuffer[i]);
                delay(100);
            }   
            
            //begin the UDP packet
            Udp.beginPacket(Udp.remoteIP(), 24);
            Udp.write(replyBuffer, UDP_TX_PACKET_MAX_SIZE);
            //go through buffer and throwing it into the UDP packet to be sent 
            Serial1.write(Udp.remoteIP());
            Serial1.write(Udp.remoteIP()[1]);
            Serial1.write(Udp.remoteIP()[2]);
            Serial1.write(Udp.remoteIP()[3]);
            //Writes 192.168.123.12
            Serial1.write(Udp.remotePort());
            
            //end packet so it will now be sent
            Serial1.write(Udp.endPacket());
            for(int i = 0; i < UDP_TX_PACKET_MAX_SIZE; i++) {
            //reset the packet buffer so its empty
                replyBuffer[i] = 0;
            }
            size = 0;
            count = 0;
        }
    }
}

void readPackets() {
    int packetSize;
    //Have we any data to read.
    if((packetSize = Udp.parsePacket())) {
        //Serial.print("Recieved Packet of Size: ");
        //Serial.println(packetSize);
        //Serial.print("From: ");
        IPAddress remote = Udp.remoteIP();
        for (int i = 0; i < 4; i++) {
            //Serial.print(remote[i], DEC);
            if (i < 3) {
                //Serial.print(".");
            }
        }
        //Serial.print(", port ");
        //Serial.println(Udp.remotePort());

        //Read the packet into the Packet Buffer.
        Udp.read(packetBuffer, UDP_TX_PACKET_MAX_SIZE);
        //Serial.println("Contents:");
        //Serial.println((byte*)packetBuffer);

        //Now we send a serial reply...
        for(int i = 0; i < packetSize; i++) {
            Serial.write(packetBuffer[i]);
            //Serial.print("Wrote: ");
            //Serial.println(packetBuffer[i]);
            delay(10);
        }
    }
}
