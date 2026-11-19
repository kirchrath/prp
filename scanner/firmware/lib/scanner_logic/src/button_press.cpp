#include "button_press.h"

namespace logic {

Direction ButtonPress::update(bool pressed, uint32_t now) {
  if (pressed != raw_) {
    raw_ = pressed;
    rawSince_ = now;
  }
  if (raw_ != stable_ && now - rawSince_ >= kDebounceMs) {
    stable_ = raw_;
    if (stable_) {
      pressedAt_ = now;
      longFired_ = false;
    } else if (!longFired_) {
      return Direction::In;  // released before the long press time
    }
  }
  if (stable_ && !longFired_ && now - pressedAt_ >= kLongPressMs) {
    longFired_ = true;
    return Direction::Out;
  }
  return Direction::None;
}

}  // namespace logic
