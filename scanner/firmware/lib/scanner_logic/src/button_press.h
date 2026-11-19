#pragma once

#include <cstdint>

#include "direction.h"

namespace logic {

/**
 * One button of a button box: short press = In (one unit stored), long press = Out (one unit taken).
 * Debounced; the long press fires as soon as the button has been held long enough, releasing it afterwards does
 * nothing. Pure logic, fed with the raw pin state and the time.
 */
class ButtonPress {
 public:
  static constexpr uint32_t kDebounceMs = 30;
  static constexpr uint32_t kLongPressMs = 1000;

  /** Returns In/Out when a press is complete, otherwise None. */
  Direction update(bool pressed, uint32_t now);

 private:
  bool stable_ = false;      // debounced state
  bool raw_ = false;         // last raw reading
  uint32_t rawSince_ = 0;    // when the raw reading last changed
  uint32_t pressedAt_ = 0;   // when the debounced press started
  bool longFired_ = false;
};

}  // namespace logic
