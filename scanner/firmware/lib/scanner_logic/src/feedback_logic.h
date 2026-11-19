#pragma once
#include <cstdint>
#include <string>

namespace logic {

// Visual/acoustic signals of the scanner (see docs/hardware.md)
enum class Signal {
  Idle,         // LED off
  Waiting,      // dim white: sent, waiting for the server
  InOk,         // green, high beep
  InUnknown,    // yellow, two beeps: stored, complete the article in the inbox
  OutOk,        // blue, short beep
  Error,        // red, long low beep
  NoDirection,  // magenta: barcode read but no direction detected
  Offline,      // slow orange blinking: no WiFi/MQTT
  Queued,       // cyan: stored locally, sent when online again
};

/** Signal for the "result" of a feedback message. */
Signal signalForResult(const std::string& result);

/** Tracks the scan waiting for its feedback; PENDING_ARTICLE extends the wait (article is being created). */
class FeedbackTimer {
 public:
  static constexpr uint32_t kTimeoutMs = 5000;
  static constexpr uint32_t kPendingTimeoutMs = 20000;

  void sent(uint32_t seq, uint32_t now);
  /** Feedback received; returns the signal to show. */
  Signal received(uint32_t seq, const std::string& result, uint32_t now);
  /** True once when the awaited feedback did not arrive in time. */
  bool timedOut(uint32_t now);

 private:
  uint32_t seq_ = 0, since_ = 0, timeout_ = kTimeoutMs;
};

}  // namespace logic
