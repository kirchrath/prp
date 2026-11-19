#include "pairing.h"

namespace logic {

void ScanPairer::barcode(const std::string& code, uint32_t now) {
  bool repeated = hasLastCode_ && code == lastCode_ && now - lastCodeAt_ <= samePause_;
  lastCode_ = code;
  lastCodeAt_ = now;
  hasLastCode_ = true;
  if (repeated) return;
  code_ = code;
  codeAt_ = now;
  hasCode_ = true;
}

void ScanPairer::move(Direction d, uint32_t now) {
  move_ = d;
  moveAt_ = now;
}

PairResult ScanPairer::poll(uint32_t now) {
  PairResult result;
  bool hasMove = move_ != Direction::None;
  if (hasCode_ && hasMove) {
    uint32_t diff = codeAt_ > moveAt_ ? codeAt_ - moveAt_ : moveAt_ - codeAt_;
    if (diff <= window_) {
      result.kind = PairResult::Submit;
      result.ean = code_;
      result.direction = move_;
      hasCode_ = false;
      move_ = Direction::None;
      return result;
    }
  }
  if (hasCode_ && now - codeAt_ > window_) {
    result.kind = PairResult::NoDirection;
    result.ean = code_;
    hasCode_ = false;
  }
  if (hasMove && now - moveAt_ > window_) move_ = Direction::None;
  return result;
}

}  // namespace logic
