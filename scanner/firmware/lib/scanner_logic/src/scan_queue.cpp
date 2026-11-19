#include "scan_queue.h"

namespace logic {

bool ScanQueue::push(const QueuedScan& scan) {
  if (count_ == kCapacity) return false;
  items_[(head_ + count_) % kCapacity] = scan;
  count_++;
  return true;
}

void ScanQueue::pop() {
  if (count_ == 0) return;
  head_ = (head_ + 1) % kCapacity;
  count_--;
}

}  // namespace logic
