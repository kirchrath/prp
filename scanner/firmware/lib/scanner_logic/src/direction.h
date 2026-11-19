#pragma once
#include <cstdint>

namespace logic {

enum class Direction { None, In, Out };

/** Opposite direction if {@code invert} is set (sensors mounted the other way round). */
Direction applyInvert(Direction d, bool invert);

/**
 * Detects the movement direction from the order in which two sensors are triggered.
 * Sensor A is on the room side, sensor B on the shelf side: A then B = in, B then A = out.
 */
class DirectionDetector {
 public:
  /** Feed the current sensor states (true = object detected); returns a direction once per completed pass. */
  Direction update(bool a, bool b, uint32_t now);

  static constexpr uint32_t kPassTimeoutMs = 2000;  // a pass must complete within this time
  static constexpr uint32_t kClearMs = 80;          // both sensors free this long = pass finished

 private:
  uint32_t firstA_ = 0, firstB_ = 0, lastActive_ = 0;
  bool seenA_ = false, seenB_ = false;
  void reset() { seenA_ = seenB_ = false; }
};

}  // namespace logic
