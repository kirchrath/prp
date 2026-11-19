#include "direction.h"

// a pass is abandoned if it does not complete within this time
static const uint32_t PASS_TIMEOUT_MS = 2000;
// both sensors must be free for this long before a pass counts as finished
static const uint32_t CLEAR_MS = 80;

void DirectionDetector::begin(int pinA, int pinB) {
  pinA_ = pinA;
  pinB_ = pinB;
  pinMode(pinA_, INPUT_PULLUP);
  pinMode(pinB_, INPUT_PULLUP);
}

Direction DirectionDetector::poll() {
  uint32_t now = millis();
  bool a = active(pinA_);
  bool b = active(pinB_);
  if (a && firstA_ == 0) firstA_ = now;
  if (b && firstB_ == 0) firstB_ = now;
  if (a || b) lastActive_ = now;

  if (firstA_ == 0 && firstB_ == 0) return Direction::None;

  uint32_t started = firstA_ && firstB_ ? min(firstA_, firstB_) : (firstA_ ? firstA_ : firstB_);
  if (now - started > PASS_TIMEOUT_MS && (a || b)) {
    // something stays in front of the sensors (e.g. a hand resting) - ignore this pass
    reset();
    return Direction::None;
  }

  if (!a && !b && now - lastActive_ >= CLEAR_MS) {
    Direction d = Direction::None;
    if (firstA_ && firstB_ && firstA_ != firstB_) {
      d = firstA_ < firstB_ ? Direction::In : Direction::Out;
    }
    reset();  // only one sensor triggered or both at once: no direction
    return d;
  }
  return Direction::None;
}
