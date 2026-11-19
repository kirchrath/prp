#include "feedback_logic.h"

namespace logic {

Signal signalForResult(const std::string& result) {
  if (result == "IN_OK" || result == "IN_OK_NEW_ARTICLE") return Signal::InOk;
  if (result == "IN_OK_UNKNOWN_ARTICLE") return Signal::InUnknown;
  if (result == "OUT_OK") return Signal::OutOk;
  if (result == "PENDING_ARTICLE") return Signal::Waiting;
  return Signal::Error;
}

void FeedbackTimer::sent(uint32_t seq, uint32_t now) {
  seq_ = seq;
  since_ = now;
  timeout_ = kTimeoutMs;
}

Signal FeedbackTimer::received(uint32_t seq, const std::string& result, uint32_t now) {
  Signal signal = signalForResult(result);
  if (seq == seq_) {
    if (signal == Signal::Waiting) {
      since_ = now;
      timeout_ = kPendingTimeoutMs;
    } else {
      seq_ = 0;
    }
  }
  return signal;
}

bool FeedbackTimer::timedOut(uint32_t now) {
  if (seq_ == 0 || now - since_ <= timeout_) return false;
  seq_ = 0;
  return true;
}

}  // namespace logic
