#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <Servo.h>

// Edite com suas credenciais Wi-Fi
const char* ssid     = "SEU_SSID";
const char* password = "SUA_SENHA";

ESP8266WebServer server(80);

Servo servoPan;
Servo servoTilt;

const int pinPan  = D5;
const int pinTilt = D6;

int pan  = 90;
int tilt = 90;

void handleRoot() {
  server.send(200, "text/plain", "FPV Lupus Servo OK");
}

void handleControl() {
  if (server.hasArg("pan")) {
    pan = server.arg("pan").toInt();
  }

  if (server.hasArg("tilt")) {
    tilt = server.arg("tilt").toInt();
  }

  pan  = constrain(pan,  40, 140);
  tilt = constrain(tilt, 60, 120);

  servoPan.write(pan);
  servoTilt.write(tilt);

  server.send(200, "text/plain", "OK");
}

void setup() {
  Serial.begin(115200);

  servoPan.attach(pinPan);
  servoTilt.attach(pinTilt);

  servoPan.write(pan);
  servoTilt.write(tilt);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  Serial.println();
  Serial.print("IP NodeMCU: ");
  Serial.println(WiFi.localIP());

  server.on("/", handleRoot);
  server.on("/control", handleControl);

  server.begin();
}

void loop() {
  server.handleClient();
}
