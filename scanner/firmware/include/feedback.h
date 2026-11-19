#pragma once
#include <Arduino.h>

// Visual/acoustic signals of the scanner (see docs/scanner-hardware.md)
enum class Signal {
  Idle,            // LED off (or dim heartbeat when offline)
  Waiting,         // dim white: scan sent, waiting for the server
  InOk,            // green, high beep
  InUnknown,       // yellow, two beeps: stored, article must be completed in the inbox
  OutOk,           // blue, short beep
  Error,           // red, long low beep: server rejected (unknown EAN, not in stock, no location, ...)
  NoDirection,     // magenta, low beep: barcode read but direction not detected
  Offline,         // slow orange blinking: no WiFi/MQTT
  Queued,          // cyan: stored locally, will be sent when online again
};

void feedbackBegin();
void feedbackShow(Signal s);
void feedbackLoop(bool online);
