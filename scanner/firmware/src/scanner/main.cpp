// ESP32 barcode scanner for the home inventory.
//
// A GM65 module reads the barcode, two IR sensors detect in which direction the item is moved
// through the scanner. Barcode + direction are published via MQTT to the device service, which
// forwards the scan to the stock service and answers with a feedback message.
// The hardware independent logic lives in lib/scanner_logic (unit tests: pio test -e native).
// Protocol: docs/mqtt.md, wiring: docs/scanner-hardware.md

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WiFiManager.h>

#include "barcode_reader.h"
#include "direction.h"
#include "feedback.h"
#include "feedback_logic.h"
#include "gtin.h"
#include "pairing.h"
#include "pins.h"
#include "protocol.h"
#include "scan_queue.h"
#include "settings.h"

#ifndef FIRMWARE_VERSION
#define FIRMWARE_VERSION "dev"
#endif

using logic::Direction;
using logic::Signal;

static const char *TOPIC_PREFIX = "inventory/scanner/";

static Settings settings;
static WiFiClient wifiClient;
static PubSubClient mqtt(wifiClient);
static HardwareSerial scannerSerial(2);

static logic::DirectionDetector detector;
static logic::ScanPairer pairer;
static logic::BarcodeReader reader;
static logic::FeedbackTimer feedbackTimer;
static logic::ScanQueue queue;

static std::string bootId;
static uint32_t seq = 0;

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
  bool ok = forcePortal || settings.mqttHost.isEmpty() ? wm.startConfigPortal(ap.c_str()) : wm.autoConnect(ap.c_str());
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

static bool publishScan(const std::string &ean, Direction dir, uint32_t scanSeq) {
  return mqtt.publish(topic("scan").c_str(), logic::scanMessage(ean, dir, scanSeq, bootId).c_str());
}

static void onMessage(char *, byte *payload, unsigned int length) {
  logic::Feedback feedback;
  if (!logic::parseFeedback(reinterpret_cast<const char *>(payload), length, feedback)) return;
  Serial.printf("Feedback seq=%u result=%s article=%s\n", feedback.seq, feedback.result.c_str(), feedback.article.c_str());
  feedbackShow(feedbackTimer.received(feedback.seq, feedback.result, millis()));
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
  std::string status = logic::statusMessage(FIRMWARE_VERSION, WiFi.localIP().toString().c_str(), WiFi.RSSI(), bootId,
                                             "scanner");
  mqtt.publish(willTopic.c_str(), status.c_str(), true);

  // send everything that was scanned while offline
  while (!queue.empty() && publishScan(queue.front().ean, queue.front().direction, queue.front().seq)) {
    queue.pop();
  }
}

// ---------------------------------------------------------------------------------------------
// scanning
// ---------------------------------------------------------------------------------------------

static void submit(const std::string &ean, Direction dir) {
  dir = logic::applyInvert(dir, settings.invertDirection);
  uint32_t scanSeq = ++seq;
  Serial.printf("Scan %s %s seq=%u\n", ean.c_str(), dir == Direction::In ? "IN" : "OUT", scanSeq);
  if (mqtt.connected() && publishScan(ean, dir, scanSeq)) {
    feedbackTimer.sent(scanSeq, millis());
    feedbackShow(Signal::Waiting);
  } else if (queue.push({ean, dir, scanSeq})) {
    feedbackShow(Signal::Queued);
  } else {
    feedbackShow(Signal::Error);
  }
}

// ---------------------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(PIN_CONFIG_BUTTON, INPUT_PULLUP);
  pinMode(PIN_SENSOR_A, INPUT_PULLUP);
  pinMode(PIN_SENSOR_B, INPUT_PULLUP);
  feedbackBegin();
  scannerSerial.begin(SCANNER_BAUD, SERIAL_8N1, PIN_SCANNER_RX, PIN_SCANNER_TX);

  char id[9];
  snprintf(id, sizeof(id), "%08lx", static_cast<unsigned long>(esp_random()));
  bootId = id;

  loadSettings(settings);
  Serial.printf("Inventory scanner %s, device %s\n", FIRMWARE_VERSION, settings.deviceId.c_str());
  startWifi(digitalRead(PIN_CONFIG_BUTTON) == LOW);

  mqtt.setServer(settings.mqttHost.c_str(), settings.mqttPort);
  mqtt.setBufferSize(512);
  mqtt.setKeepAlive(30);
  mqtt.setCallback(onMessage);
}

void loop() {
  uint32_t now = millis();
  checkConfigButton();
  ensureMqtt();
  mqtt.loop();

  std::string code;
  while (scannerSerial.available()) {
    if (reader.feed(static_cast<char>(scannerSerial.read()), now, code)) pairer.barcode(code, now);
  }
  if (reader.poll(now, code)) pairer.barcode(code, now);

  // sensors are active LOW
  Direction dir = detector.update(digitalRead(PIN_SENSOR_A) == LOW, digitalRead(PIN_SENSOR_B) == LOW, now);
  if (dir != Direction::None) pairer.move(dir, now);

  logic::PairResult pair = pairer.poll(now);
  if (pair.kind == logic::PairResult::Submit && !logic::isValidGtin(pair.ean)) {
    Serial.printf("Barcode %s is not a valid EAN - discarded\n", pair.ean.c_str());
    feedbackShow(Signal::Error);
  } else if (pair.kind == logic::PairResult::Submit) {
    submit(pair.ean, pair.direction);
  } else if (pair.kind == logic::PairResult::NoDirection) {
    Serial.printf("Barcode %s without direction - discarded\n", pair.ean.c_str());
    feedbackShow(Signal::NoDirection);
  }

  if (feedbackTimer.timedOut(now)) {
    Serial.println("No feedback from server");
    feedbackShow(Signal::Error);
  }

  feedbackLoop(mqtt.connected());
  delay(2);
}
