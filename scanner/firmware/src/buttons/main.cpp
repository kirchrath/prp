// ESP32 button box for the home inventory: articles without barcode (vegetables, fruit, ...).
//
// Up to 8 buttons, each assigned to an article in the web app (Einstellungen -> Geräte).
// Short press = one unit stored, long press (1 s) = one unit taken. The press is published via MQTT to the
// device service, which books it in the stock service and answers with a feedback message (LED + buzzer).
// Shares WiFi/MQTT setup, feedback, settings and the offline queue with the scanner firmware.
// Logic (debounce, short/long press): lib/scanner_logic/button_press.h (unit tests: pio test -e native)
// Protocol: docs/mqtt.md, wiring: docs/hardware.md

#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFi.h>
#include <WiFiManager.h>

#include "button_pins.h"
#include "button_press.h"
#include "feedback.h"
#include "feedback_logic.h"
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

static logic::ButtonPress buttons[BUTTON_COUNT];
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
  wm.addParameter(&pHost);
  wm.addParameter(&pPort);
  wm.addParameter(&pDevice);
  wm.addParameter(&pPass);

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

static bool publishPress(int button, Direction dir, uint32_t pressSeq) {
  return mqtt.publish(topic("button").c_str(), logic::buttonMessage(button, dir, pressSeq, bootId).c_str());
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
                                            "buttons");
  mqtt.publish(willTopic.c_str(), status.c_str(), true);

  // send everything that was pressed while offline
  while (!queue.empty() && publishPress(queue.front().button, queue.front().direction, queue.front().seq)) {
    queue.pop();
  }
}

static void submit(int button, Direction dir) {
  uint32_t pressSeq = ++seq;
  Serial.printf("Button %d %s seq=%u\n", button, dir == Direction::In ? "IN" : "OUT", pressSeq);
  if (mqtt.connected() && publishPress(button, dir, pressSeq)) {
    feedbackTimer.sent(pressSeq, millis());
    feedbackShow(Signal::Waiting);
  } else if (queue.push({"", dir, pressSeq, button})) {
    feedbackShow(Signal::Queued);
  } else {
    feedbackShow(Signal::Error);
  }
}

// ---------------------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(PIN_CONFIG_BUTTON, INPUT_PULLUP);
  for (int pin : BUTTON_PINS) pinMode(pin, INPUT_PULLUP);
  feedbackBegin();

  char id[9];
  snprintf(id, sizeof(id), "%08lx", static_cast<unsigned long>(esp_random()));
  bootId = id;

  loadSettings(settings);
  Serial.printf("Inventory button box %s, device %s\n", FIRMWARE_VERSION, settings.deviceId.c_str());
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

  // buttons are active LOW, numbered from 1
  for (int i = 0; i < BUTTON_COUNT; i++) {
    Direction dir = buttons[i].update(digitalRead(BUTTON_PINS[i]) == LOW, now);
    if (dir != Direction::None) submit(i + 1, dir);
  }

  if (feedbackTimer.timedOut(now)) {
    Serial.println("No feedback from server");
    feedbackShow(Signal::Error);
  }

  feedbackLoop(mqtt.connected());
  delay(2);
}
