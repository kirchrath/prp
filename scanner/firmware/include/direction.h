#pragma once
#include <Arduino.h>

enum class Direction { None, In, Out };

// Detects the movement direction from the order in which two sensors are triggered.
class DirectionDetector {
 public:
  void begin(int pinA, int pinB);
  // Call frequently; returns a direction once per completed pass.
  Direction poll();

 private:
  int pinA_ = -1, pinB_ = -1;
  uint32_t firstA_ = 0, firstB_ = 0;  // millis of first trigger in the current pass, 0 = not triggered
  uint32_t lastActive_ = 0;
  bool active(int pin) const { return digitalRead(pin) == LOW; }
  void reset() { firstA_ = firstB_ = 0; }
};
