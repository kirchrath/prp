#include "settings.h"
#include <Preferences.h>

static const char *NS = "scanner";

#ifndef DEVICE_ID_PREFIX
#define DEVICE_ID_PREFIX "scanner"
#endif

static String defaultDeviceId() {
  uint64_t mac = ESP.getEfuseMac();
  char id[24];
  snprintf(id, sizeof(id), DEVICE_ID_PREFIX "-%06x", static_cast<uint32_t>(mac >> 24) & 0xFFFFFF);
  return String(id);
}

void loadSettings(Settings &s) {
  Preferences p;
  p.begin(NS, true);
  s.mqttHost = p.getString("host", "");
  s.mqttPort = p.getUShort("port", 1883);
  s.deviceId = p.getString("device", defaultDeviceId());
  s.mqttPassword = p.getString("pass", "");
  s.invertDirection = p.getBool("invert", false);
  p.end();
}

void saveSettings(const Settings &s) {
  Preferences p;
  p.begin(NS, false);
  p.putString("host", s.mqttHost);
  p.putUShort("port", s.mqttPort);
  p.putString("device", s.deviceId);
  p.putString("pass", s.mqttPassword);
  p.putBool("invert", s.invertDirection);
  p.end();
}

void clearSettings() {
  Preferences p;
  p.begin(NS, false);
  p.clear();
  p.end();
}
