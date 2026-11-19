#pragma once
#include <cstddef>
#include <cstdint>
#include <string>
#include "direction.h"

namespace logic {

struct QueuedScan {
  std::string ean;
  Direction direction = Direction::None;
  uint32_t seq = 0;
  int button = 0;  // button box: number of the button (ean unused)
};

/** Ring buffer for scans made while offline. */
class ScanQueue {
 public:
  static constexpr size_t kCapacity = 32;

  bool push(const QueuedScan& scan);
  bool empty() const { return count_ == 0; }
  size_t size() const { return count_; }
  const QueuedScan& front() const { return items_[head_]; }
  void pop();

 private:
  QueuedScan items_[kCapacity];
  size_t head_ = 0, count_ = 0;
};

}  // namespace logic
