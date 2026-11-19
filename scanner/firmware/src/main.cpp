// ESP32 barcode scanner for the home inventory.
//
// A GM65 module reads the barcode, two IR sensors detect in which direction the item is moved
// through the scanner. Barcode + direction are published via MQTT; the backend books the item
// in or out at the location assigned to this scanner and answers with a feedback message.
// Protocol: docs/mqtt.md, wiring: docs/scanner-hardware.md

#include <Arduino.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WiFiManager.h>

#include "direction.h"
#include "feedback.h"
#include "pins.h"
#include "settings.h"

#ifndef FIRMWARE_VERSION
#define FIRMWARE_VERSION "dev"
#endif

static const char *TOPIC_PREFIX = "inventory/scanner/";
// barcode and movement must happen within this window to belong together
static const uint32_t PAIR_WINDOW_MS = 2000;
// the same barcode is ignored for this long (GM65 re-reads while the item is in view)
static const uint32_t SAME_CODE_PAUSE_MS = 1500;
// no answer from the server within this time -> error signal
static const uint32_t FEEDBACK_TIMEOUT_MS = 5000;
// scans buffered while offline
static const size_t QUEUE_SIZE = 32;

static Settings settings;
static WiFiClient wifiClient;
static PubSubClient mqtt(wifiClient);
static DirectionDetector detector;
static HardwareSerial scannerSerial(2);

static String bootId;
static uint32_t seq = 0;

struct PendingBarcode {
  String code;
  uint32_t at = 0;
};
struct PendingMove {
  Direction dir = Direction::None;
  uint32_t at = 0;
};
struct QueuedScan {
  String ean;
  Direction dir;
  uint32_t seq;
};

static PendingBarcode pendingBarcode;
static PendingMove pendingMove;
static String lastCode;
static uint32_t lastCodeAt = 0;
static uint32_t awaitingSeq = 0;
static uint32_t awaitingSince = 0;
static QueuedScan queue[QUEUE_SIZE];
static size_t queueHead = 0, queueCount = 0;

static String topic(const char *suffix) {
  return String(TOPIC_PREFIX) + settings.deviceId + "/" + suffix;
}

// ---------------------------------------------------------------------------------------------
// configuration portal
// ---------------------------------------------------------------------------------------------

static void startWifi(bool forcePortal) {
  WiFiManager wm;
  char port[6];
  snprintf(port, sizeof(port), "%u", settings.mqttPort);
  WiFiManagerParameter pHost("mqtt_host", "MQTT-Broker (Hostname/IP)", settings.mqttHost.c_str(), 64);
  WiFiManagerParameter pPort("mqtt_port", "MQTT-Port", port, 5);
  WiFiManagerParameter pDevice("device_id", "Geräte-ID = MQTT-Benutzer", settings.deviceId.c_str(), 32);
  WiFiManagerParameter pPass("mqtt_pass", "MQTT-Passwort", settings.mqttPassword.c_str(), 64);
  WiFiManagerParameter pInvert("invert", "Richtung invertieren (1 = ja)", settings.invertDirection ? "1" : "0", 1);
  wm.addParameter(&pHost);
  wm.addParameter(&pPort);
  wm.addParameter(&pDevice);
  wm.addParameter(&pPass);
  wm.addParameter(&pInvert);

  bool saved = false;
  wm.setSaveParamsCallback([&saved]() { saved = true; });
  wm.setSaveConfigCallback([&saved]() { saved = true; });
  wm.setConfigPortalTimeout(300);

  String ap = "Vorrat-" + settings.deviceId;
  bool ok = forcePortal || settings.mqttHost.isEmpty() ? wm.startConfigPortal(ap.c_str())
                                                       : wm.autoConnect(ap.c_str());
  if (saved) {
    settings.mqttHost = pHost.getValue();
    settings.mqttPort = static_cast<uint16_t>(atoi(pPort.getValue()));
    settings.deviceId = pDevice.getValue();
    settings.mqttPassword = pPass.getValue();
    settings.invertDirection = String(pInvert.getValue()) == "1";
    saveSettings(settings);
    Serial.println("Settings saved");
  }
  if (!ok) {
    Serial.println("WiFi not configured - restarting");
    delay(1000);
    ESP.restart();
  }
}

static void checkConfigButton() {
  static uint32_t pressedSince = 0;
  if (digitalRead(PIN_CONFIG_BUTTON) == LOW) {
    if (pressedSince == 0) pressedSince = millis();
    if (millis() - pressedSince > 5000) {
      Serial.println("Config button held: clearing settings");
      feedbackShow(Signal::NoDirection);
      WiFiManager wm;
      wm.resetSettings();
      clearSettings();
      delay(500);
      ESP.restart();
    }
  } else {
    pressedSince = 0;
  }
}

// ---------------------------------------------------------------------------------------------
// MQTT
// ---------------------------------------------------------------------------------------------

static void publishStatus() {
  JsonDocument doc;
  doc["online"] = true;
  doc["fw"] = FIRMWARE_VERSION;
  doc["ip"] = WiFi.localIP().toString();
  doc["rssi"] = WiFi.RSSI();
  doc["bootId"] = bootId;
  String payload;
  serializeJson(doc, payload);
  mqtt.publish(topic("status").c_str(), payload.c_str(), true);
}

static bool publishScan(const String &ean, Direction dir, uint32_t scanSeq) {
  JsonDocument doc;
  doc["ean"] = ean;
  doc["direction"] = dir == Direction::In ? "IN" : "OUT";
  doc["seq"] = scanSeq;
  doc["bootId"] = bootId;
  String payload;
  serializeJson(doc, payload);
  return mqtt.publish(topic("scan").c_str(), payload.c_str());
}

static void onMessage(char *t, byte *payload, unsigned int length) {
  JsonDocument doc;
  if (deserializeJson(doc, payload, length)) return;
  uint32_t answeredSeq = doc["seq"] | 0;
  String result = doc["result"] | "ERROR";
  Serial.printf("Feedback seq=%u result=%s article=%s\n", answeredSeq, result.c_str(), (const char *)(doc["article"] | ""));

  // answers to scans that were queued while offline only light up briefly
  if (answeredSeq == awaitingSeq) awaitingSeq = 0;

  if (result == "IN_OK" || result == "IN_OK_NEW_ARTICLE") feedbackShow(Signal::InOk);
  else if (result == "IN_OK_UNKNOWN_ARTICLE") feedbackShow(Signal::InUnknown);
  else if (result == "OUT_OK") feedbackShow(Signal::OutOk);
  else feedbackShow(Signal::Error);
}

static void ensureMqtt() {
  static uint32_t lastAttempt = 0;
  if (mqtt.connected() || WiFi.status() != WL_CONNECTED) return;
  if (millis() - lastAttempt < 3000) return;
  lastAttempt = millis();

  Serial.printf("MQTT connect to %s:%u as %s\n", settings.mqttHost.c_str(), settings.mqttPort, settings.deviceId.c_str());
  String willTopic = topic("status");
  bool ok = mqtt.connect(settings.deviceId.c_str(), settings.deviceId.c_str(), settings.mqttPassword.c_str(),
                         willTopic.c_str(), 1, true, "{\"online\":false}");
  if (!ok) {
    Serial.printf("MQTT connect failed, state=%d\n", mqtt.state());
    return;
  }
  mqtt.subscribe(topic("feedback").c_str(), 1);
  publishStatus();

  // send everything that was scanned while offline
  while (queueCount > 0) {
    QueuedScan &q = queue[queueHead];
    if (!publishScan(q.ean, q.dir, q.seq)) break;
    queueHead = (queueHead + 1) % QUEUE_SIZE;
    queueCount--;
  }
}

// ---------------------------------------------------------------------------------------------
// scanning
// ---------------------------------------------------------------------------------------------

// Reads one barcode from the GM65. Codes are terminated by CR/LF or a pause of 50 ms.
static bool readBarcode(String &out) {
  static String buffer;
  static uint32_t lastByte = 0;
  while (scannerSerial.available()) {
    char c = static_cast<char>(scannerSerial.read());
    lastByte = millis();
    if (c == '\r' || c == '\n') {
      if (!buffer.isEmpty()) {
        out = buffer;
        buffer = "";
        return true;
      }
    } else if (isPrintable(c) && buffer.length() < 64) {
      buffer += c;
    }
  }
  if (!buffer.isEmpty() && millis() - lastByte > 50) {
    out = buffer;
    buffer = "";
    return true;
  }
  return false;
}

static void submit(const String &ean, Direction dir) {
  if (settings.invertDirection) dir = dir == Direction::In ? Direction::Out : Direction::In;
  uint32_t scanSeq = ++seq;
  Serial.printf("Scan %s %s seq=%u\n", ean.c_str(), dir == Direction::In ? "IN" : "OUT", scanSeq);
  if (mqtt.connected() && publishScan(ean, dir, scanSeq)) {
    awaitingSeq = scanSeq;
    awaitingSince = millis();
    feedbackShow(Signal::Waiting);
  } else if (queueCount < QUEUE_SIZE) {
    queue[(queueHead + queueCount) % QUEUE_SIZE] = {ean, dir, scanSeq};
    queueCount++;
    feedbackShow(Signal::Queued);
  } else {
    feedbackShow(Signal::Error);
  }
}

// Combines barcode and movement that happened close together (in any order).
static void pairEvents() {
  uint32_t now = millis();
  bool haveCode = !pendingBarcode.code.isEmpty();
  bool haveMove = pendingMove.dir != Direction::None;

  if (haveCode && haveMove) {
    uint32_t diff = pendingBarcode.at > pendingMove.at ? pendingBarcode.at - pendingMove.at
                                                       : pendingMove.at - pendingBarcode.at;
    if (diff <= PAIR_WINDOW_MS) {
      submit(pendingBarcode.code, pendingMove.dir);
      pendingBarcode = {};
      pendingMove = {};
      return;
    }
  }
  if (haveCode && now - pendingBarcode.at > PAIR_WINDOW_MS) {
    Serial.printf("Barcode %s without direction - discarded\n", pendingBarcode.code.c_str());
    feedbackShow(Signal::NoDirection);
    pendingBarcode = {};
  }
  if (haveMove && now - pendingMove.at > PAIR_WINDOW_MS) {
    pendingMove = {};  // something moved without a barcode (e.g. a hand) - ignore silently
  }
}

// ---------------------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(PIN_CONFIG_BUTTON, INPUT_PULLUP);
  feedbackBegin();
  detector.begin(PIN_SENSOR_A, PIN_SENSOR_B);
  scannerSerial.begin(SCANNER_BAUD, SERIAL_8N1, PIN_SCANNER_RX, PIN_SCANNER_TX);

  char id[9];
  snprintf(id, sizeof(id), "%08x", esp_random());
  bootId = id;

  loadSettings(settings);
  Serial.printf("Inventory scanner %s, device %s\n", FIRMWARE_VERSION, settings.deviceId.c_str());
  bool forcePortal = digitalRead(PIN_CONFIG_BUTTON) == LOW;
  startWifi(forcePortal);

  mqtt.setServer(settings.mqttHost.c_str(), settings.mqttPort);
  mqtt.setBufferSize(512);
  mqtt.setKeepAlive(30);
  mqtt.setCallback(onMessage);
}

void loop() {
  checkConfigButton();
  ensureMqtt();
  mqtt.loop();

  String code;
  if (readBarcode(code)) {
    uint32_t now = millis();
    if (code != lastCode || now - lastCodeAt > SAME_CODE_PAUSE_MS) {
      pendingBarcode = {code, now};
    }
    lastCode = code;
    lastCodeAt = now;
  }

  Direction dir = detector.poll();
  if (dir != Direction::None) {
    pendingMove = {dir, millis()};
  }

  pairEvents();

  if (awaitingSeq != 0 && millis() - awaitingSince > FEEDBACK_TIMEOUT_MS) {
    Serial.println("No feedback from server");
    awaitingSeq = 0;
    feedbackShow(Signal::Error);
  }

  feedbackLoop(mqtt.connected());
  delay(2);
}
