#include "barcode_reader.h"

namespace logic {

bool BarcodeReader::take(std::string& out) {
  if (buffer_.empty()) return false;
  out = buffer_;
  buffer_.clear();
  return true;
}

bool BarcodeReader::feed(char c, uint32_t now, std::string& out) {
  lastByte_ = now;
  if (c == '\r' || c == '\n') return take(out);
  if (c >= 0x20 && c < 0x7f && buffer_.size() < kMaxLength) buffer_ += c;
  return false;
}

bool BarcodeReader::poll(uint32_t now, std::string& out) {
  return now - lastByte_ > kPauseMs && take(out);
}

}  // namespace logic
