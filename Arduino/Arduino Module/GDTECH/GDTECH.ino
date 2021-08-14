/*
  SimpleMQTTClient.ino
  The purpose of this exemple is to illustrate a simple handling of MQTT and Wifi connection.
  Once it connects successfully to a Wifi network and a MQTT broker, it subscribe to a topic and send a message to it.
  It will also send a message delayed 5 seconds later.
*/
#include <MQ2Lib.h>

//change this with the pin that you use
int pin = A0;
int lpg=0, co=0, smoke=0;
int pin_trigger = 14;
int pin_fan = 13;
int pin_light_safe = 5;
int pin_light_warning = 12 ;
int pin_light_danger = 0;
int pin_buzzer = 16;
//int pin_fan_ground = 13;
boolean connected=false;

MQ2 mq2(pin, true);


#include "EspMQTTClient.h"


EspMQTTClient client(
  "Zeox Portable Hotspot",// SSID
  "killzone2", //PASSWORD
  "192.168.43.201",  // MQTT SERVER IP (Host)
  "MQTTUsername",   // Can be omitted if not needed
  "MQTTPassword",   // Can be omitted if not needed
  "TestClient",     // Client name that uniquely identify your device
  1883              // The MQTT port, default to 1883. this line can be omitted
);

double lat = 7.080247;
double lng =  125.620763;
String device_name = "SENSOR_1";

void setup()
{
  Serial.begin(115200);
  mq2.begin();
  pinMode(pin_trigger, INPUT_PULLUP);
  pinMode(pin_fan, OUTPUT);
  pinMode(pin_buzzer, OUTPUT);
  pinMode(pin_light_safe, OUTPUT);
  pinMode(pin_light_warning, OUTPUT);
  pinMode(pin_light_danger, OUTPUT);
  digitalWrite(pin_buzzer,LOW);
  digitalWrite(pin_light_safe,HIGH);
  digitalWrite(pin_light_warning,HIGH);
  digitalWrite(pin_light_danger,HIGH);
//  digitalWrite(pin_fan, HIGH);
  // Optionnal functionnalities of EspMQTTClient : 
  client.enableDebuggingMessages(); // Enable debugging messages sent to serial output
  client.enableHTTPWebUpdater(); // Enable the web updater. User and password default to values of MQTTUsername and MQTTPassword. These can be overrited with enableHTTPWebUpdater("user", "password").
  client.enableLastWillMessage("mytopic/test", "{\"id\":\"SENSOR_1\", \"status\":\"disconnected\"}");  // You can activate the retain flag by setting the third parameter to true
}

// This function is called once everything is connected (Wifi and MQTT)
// WARNING : YOU MUST IMPLEMENT IT IF YOU USE EspMQTTClient
void onConnectionEstablished()
{
  
  // Subscribe to "mytopic/test" and display received message to Serial
  client.subscribe("mytopic/test", [](const String & payload) {
    Serial.println(payload);
  });

  // Publish a message to "mytopic/test"
  connected=true;
  digitalWrite(2, LOW);
  client.publish("mytopic/test", "{\"id\":\"SENSOR_1\", \"status\":\"connected\"}"); // You can activate the retain flag by setting the third parameter to true
  

//  // Execute delayed instructions
//  client.executeDelayed(5 * 1000, []() {
//    client.publish("mytopic/test", "This is a message sent 5 seconds later");//
//  });/
}

void loop()
{
  
  client.loop();
  
    float* values= mq2.read(true);
      lpg = mq2.readLPG();
      co = mq2.readCO();
      smoke = mq2.readSmoke();
      int trigger = digitalRead(pin_trigger);
      String val1;
      val1 = "{\"LPG\": \"";
      val1 += (String)lpg;
      val1 += "\",\"CO\":\"";
      val1 += (String)co;
      val1 += "\",\"SMOKE\":\"";
      val1 += (String)smoke;
      val1 += "\",\"id\":";
      val1 += "\"SENSOR_1\"";
      val1 += ",";
      val1 += "\"trigger\":\"";
      val1 += (String)trigger;
      val1 += "\",\"LPG_RAW\": \"";
      val1 += (String)values[0];
      val1 += "\",\"CO_RAW\":\"";
      val1 += (String)values[1];
      val1 += "\",\"SMOKE_RAW\":\"";
      val1 += (String)values[2];
      val1 += "\",";
      val1 += "\"lat\":\"";
      val1 += String(lat,6);
      val1 += "\",";
      val1 += "\"lng\":\"";
      val1 += String(lng,6);
      val1 += "\"}";
//      digitalWrite(pin_fan, LOW); // Turn On the Fan.
      if(digitalRead(pin_trigger)==HIGH){
        digitalWrite(pin_fan, LOW); //Turn Off the Fan. 
//        digitalWrite(pin_light_safe, LOW);
//        digitalWrite(pin_light_danger, HIGH);
//        digitalWrite(pin_light_warning, HIGH);
        digitalWrite(pin_buzzer, LOW);
      }
      if((lpg<100) && (co < 100) && (smoke < 100 )){
        digitalWrite(pin_light_safe, LOW);
        digitalWrite(pin_light_warning, HIGH);
        digitalWrite(pin_light_danger, HIGH);
      }
      if((lpg > 100 && lpg < 300) || (co > 100 && co < 300) || (smoke > 100 && smoke < 300)){
        digitalWrite(pin_light_safe, HIGH);
          digitalWrite(pin_light_warning, LOW);
          digitalWrite(pin_light_danger, HIGH);
        }else if(lpg > 300 && co > 300 && smoke > 300){
          digitalWrite(pin_light_safe, HIGH);
          digitalWrite(pin_light_warning, HIGH);
          digitalWrite(pin_light_danger, LOW);
        }
      if(digitalRead(pin_trigger)==LOW){
        digitalWrite(pin_light_safe, HIGH);
        digitalWrite(pin_fan, HIGH); // Turn On the Fan.
        
        digitalWrite(pin_buzzer, LOW);
        delay(150);
        digitalWrite(pin_buzzer,HIGH);
        delay(150);
        digitalWrite(pin_buzzer,LOW);
        delay(300);
        digitalWrite(pin_buzzer, HIGH);
        delay(150);
        
      }
      
      if(client.isConnected()){
      client.publish("mytopic/test", val1);
  }
    delay(1000);
}
