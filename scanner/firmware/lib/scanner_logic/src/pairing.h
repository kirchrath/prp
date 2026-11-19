#pragma once
#include <cstdint>
#include <string>
#include "direction.h"

namespace logic {

struct PairResult {
  enum Kind { None, Submit, NoDirection } kind = None;
  std::string ean;
  Direction direction = Direction::None;
};

/** Combines a barcode and a movement that happened close together, in any order. */
class ScanPairer {
 public:
  explicit ScanPairer(uint32_t windowMs = 2000, uint32_t sameCodePauseMs = 1500)
      : window_(windowMs), samePause_(sameCodePauseMs) {}

  /** A barcode was read; the same code is ignored for a while (the module re-reads while it is in view). */
  void barcode(const std::string& code, uint32_t now);
  void move(Direction d, uint32_t now);
  /** Call regularly: a pair is submitted, a barcode without movement is reported, a lone movement dropped. */
  PairResult poll(uint32_t now);

 private:
  uint32_t window_, samePause_;
  std::string code_, lastCode_;
  uint32_t codeAt_ = 0, lastCodeAt_ = 0, moveAt_ = 0;
  bool hasCode_ = false, hasLastCode_ = false;
  Direction move_ = Direction::None;
};

}  // namespace logic
