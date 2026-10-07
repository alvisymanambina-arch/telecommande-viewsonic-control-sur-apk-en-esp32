#include <WiFi.h>
#include <WebServer.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>

const char* ssid_ap = "ViewSonic_Remote";
const char* password_ap = "12345678";

const uint16_t kIrLed = 4; 
IRsend irsend(kIrLed);
WebServer server(80);

static bool rc5Toggle = false;
const uint16_t kRc5ToggleBit = 0x0800; 

void sendViewSonicIR(String cmd) {
  cmd.trim();
  if (cmd.length() == 0) {
    Serial.println("Commande vide recue");
    return;
  }
  cmd.toUpperCase();

  uint16_t toggle = rc5Toggle ? kRc5ToggleBit : 0x0000;

  if (cmd == "POWER")        irsend.sendRC5(0xC  | toggle, 12);
  else if (cmd == "WINDOWS")  irsend.sendRC5(0x13 | toggle, 12);
  else if (cmd == "PENCIL")   irsend.sendRC5(0x30 | toggle, 12);
  else if (cmd == "HOME")     irsend.sendRC5(0x2D | toggle, 12);
  else if (cmd == "BACK")     irsend.sendRC5(0xF  | toggle, 12);
  else if (cmd == "VOL_UP")   irsend.sendRC5(0x10 | toggle, 12);
  else if (cmd == "VOL_DOWN") irsend.sendRC5(0x11 | toggle, 12);
  else if (cmd == "MUTE")     irsend.sendRC5(0xD  | toggle, 12);
  else if (cmd == "INFO")     irsend.sendRC5(0x12 | toggle, 12);
  else if (cmd == "SOURCE")   irsend.sendRC5(0x28 | toggle, 12);
  else if (cmd == "MENU_PC")  irsend.sendRC5(0x2F | toggle, 12);
  else if (cmd == "EXIT_PC")  irsend.sendRC5(0x14 | toggle, 12);
  else if (cmd == "SETTINGS") irsend.sendRC5(0x2E | toggle, 12);
  else if (cmd == "OK")       irsend.sendRC5(0xA  | toggle, 12);
  else if (cmd == "UP")       irsend.sendRC5(0x1C | toggle, 12);
  else if (cmd == "DOWN")     irsend.sendRC5(0x1D | toggle, 12);
  else if (cmd == "LEFT")     irsend.sendRC5(0x2C | toggle, 12);
  else if (cmd == "RIGHT")    irsend.sendRC5(0x2B | toggle, 12);
  else if (cmd == "BR_UP")    irsend.sendRC5(0x20 | toggle, 12);
  else if (cmd == "BR_DOWN")  irsend.sendRC5(0x21 | toggle, 12);
  else if (cmd == "BLANK")    irsend.sendRC5(0x26 | toggle, 12);

  else if (cmd == "0") irsend.sendRC5(0x0 | toggle, 12);
  else if (cmd == "1") irsend.sendRC5(0x1 | toggle, 12);
  else if (cmd == "2") irsend.sendRC5(0x2 | toggle, 12);
  else if (cmd == "3") irsend.sendRC5(0x3 | toggle, 12);
  else if (cmd == "4") irsend.sendRC5(0x4 | toggle, 12);
  else if (cmd == "5") irsend.sendRC5(0x5 | toggle, 12);
  else if (cmd == "6") irsend.sendRC5(0x6 | toggle, 12);
  else if (cmd == "7") irsend.sendRC5(0x7 | toggle, 12);
  else if (cmd == "8") irsend.sendRC5(0x8 | toggle, 12);
  else if (cmd == "9") irsend.sendRC5(0x9 | toggle, 12);

  else if (cmd == "RED")    irsend.sendRC5(0x32 | toggle, 12);
  else if (cmd == "GREEN")  irsend.sendRC5(0x33 | toggle, 12);
  else if (cmd == "YELLOW") irsend.sendRC5(0x34 | toggle, 12);
  else if (cmd == "BLUE")   irsend.sendRC5(0x35 | toggle, 12);

  else if (cmd == "CAPTURE") irsend.sendRC5(0x101E, 13);
  else if (cmd == "FREEZE")  irsend.sendRC5(0x101F, 13);
  else if (cmd == "ASPECT")  irsend.sendRC5(0x103E, 13);
  else {
    Serial.println("Commande tsy fantatra: " + cmd);
    return;
  }

  rc5Toggle = !rc5Toggle;

  Serial.println("Signal IR nalefa: " + cmd + " (toggle=" + String(toggle ? 1 : 0) + ")");
}

void handleCommandRequest() {
  if (!server.hasArg("val")) {
    server.sendHeader("Connection", "close");
    server.send(400, "text/plain", "Missing val");
    return;
  }

  String cmd = server.arg("val");
  cmd.trim();
  if (cmd.length() == 0) {
    server.sendHeader("Connection", "close");
    server.send(400, "text/plain", "Empty val");
    return;
  }

  sendViewSonicIR(cmd);
  server.sendHeader("Connection", "close");
  server.send(200, "text/plain", "OK");
}

void setup() {
  Serial.begin(115200);
  irsend.begin();
  WiFi.softAP(ssid_ap, password_ap);

  server.on("/cmd", HTTP_GET, handleCommandRequest);

  server.begin();
  Serial.println("ESP32 ViewSonic Ready!");
  Serial.print("AP IP address: ");
  Serial.println(WiFi.softAPIP());
}

void loop() {
  server.handleClient();
}
