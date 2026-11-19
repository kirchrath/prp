#include "direction.h"

namespace logic {

Direction applyInvert(Direction d, bool invert) {
  if (!invert || d == Direction::None) return d;
  return d == Direction::In ? Direction::Out : Direction::In;
}

Direction DirectionDetector::update(bool a, bool b, uint32_t now) {
  if (a && !seenA_) {
    seenA_ = true;
    firstA_ = now;
  }
  if (b && !seenB_) {
    seenB_ = true;
    firstB_ = now;
  }
  if (a || b) lastActive_ = now;
  if (!seenA_ && !seenB_) return Direction::None;

  uint32_t started = !seenA_ ? firstB_ : (!seenB_ || firstA_ < firstB_ ? firstA_ : firstB_);
  if (a || b) {
    // something rests in front of the sensors (e.g. a hand): ignore this pass
    if (now - started > kPassTimeoutMs) reset();
    return Direction::None;
  }
  if (now - lastActive_ < kClearMs) return Direction::None;

  Direction d = Direction::None;
  if (seenA_ && seenB_ && firstA_ != firstB_) d = firstA_ < firstB_ ? Direction::In : Direction::Out;
  reset();  // only one sensor or both at the same time: no direction
  return d;
}

}  // namespace logic
