#pragma once
#include <Arduino.h>

struct Settings {
  String mqttHost;
  uint16_t mqttPort = 1883;
  String deviceId;      // also the MQTT user name
  String mqttPassword;
  bool invertDirection = false;
};

void loadSettings(Settings &s);
void saveSettings(const Settings &s);
void clearSettings();
