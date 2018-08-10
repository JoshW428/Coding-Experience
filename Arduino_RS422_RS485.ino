/* YourDuino SoftwareSerialExample1
   - Connect to another Arduino running "YD_SoftwareSerialExampleRS485_1Remote"
   - Connect this unit Pins 10, 11, Gnd
   - Pin 3 used for RS485 direction control
   - To other unit Pins 11,10, Gnd  (Cross over)
   - Open Serial Monitor, type in top window. 
   - Should see same characters echoed back from remote Arduino

   Questions: terry@yourduino.com 
*/

/*-----( Import needed libraries )-----*/
#include <SoftwareSerial.h>
/*-----( Declare Constants and Pin Numbers )-----*/
#define SSerialRX        10  //Serial Receive pin
#define SSerialTX        11  //Serial Transmit pin

#define SSerialTxControl 5   //RS485 Direction control

#define DE 3

#define RS485Transmit    HIGH
#define RS485Receive     LOW

#define MyFlag 9
#define OtherFlag 8

#define MySpot 

#define Pin13LED         13

/*-----( Declare objects )-----*/
SoftwareSerial RS485Serial(SSerialRX, SSerialTX); // RX, TX


/*-----( Declare Variables )-----*/
int byteReceived;
int byteSend;

void setup()   /****** SETUP: RUNS ONCE ******/
{
  // Start the built-in serial port, probably to Serial Monitor
  Serial.begin(4800);
  Serial.println("YourDuino.com SoftwareSerial remote loop example");
  Serial.println("Use Serial Monitor, type in upper window, ENTER");
  
  digitalWrite(DE, LOW);

  pinMode(Pin13LED, OUTPUT);   
  pinMode(SSerialTxControl, OUTPUT);  
  pinMode(DE, OUTPUT);  
  pinMode(MyFlag, OUTPUT);
  pinMode(OtherFlag, INPUT);
  
  digitalWrite(SSerialTxControl, RS485Receive);  // Init Transceiver   
  
  // Start the software serial port, to another device
  RS485Serial.begin(4800);   // set the data rate 

}//--(end setup )---


void loop()   /****** LOOP: RUNS CONSTANTLY ******/
{ // Show activity

if(digitalRead(OtherFlag) == LOW){
  if (Serial.available())
  {
    digitalWrite(MyFlag, HIGH);
    byteReceived = Serial.read();
    
    digitalWrite(SSerialTxControl, RS485Transmit);  // Enable RS485 Transmit   
    digitalWrite(DE, RS485Transmit);
    RS485Serial.write(byteReceived);          // Send byte to Remote Arduino
    
    digitalWrite(Pin13LED, HIGH);  // Show activity    
    delay(10);

    digitalWrite(SSerialTxControl, RS485Receive);
    digitalWrite(DE, RS485Receive);

    digitalWrite(Pin13LED, LOW);  // Disable RS485 Transmit      
    
    digitalWrite(MyFlag, LOW);
    Serial.println();
  }
  
}
  if (RS485Serial.available())  //Look for data from other Arduino
   {
    digitalWrite(Pin13LED, HIGH);  // Show activity
    byteReceived = RS485Serial.read();    // Read received byte
    Serial.write(byteReceived);        // Show on Serial Monitor
    delay(10);
    digitalWrite(Pin13LED, LOW);  // Show activity   
   }  

}//--(end main loop )---

/*-----( Declare User-written Functions )-----*/

//NONE
//*********( THE END )***********
