#pragma once
#include <cstdint>
#include <string>

namespace logic {

/** Frames the serial output of the barcode module: a code ends with CR/LF or a pause. */
class BarcodeReader {
 public:
  static constexpr size_t kMaxLength = 64;
  static constexpr uint32_t kPauseMs = 50;

  /** Feed one received byte; returns true and fills {@code out} when a code is complete. */
  bool feed(char c, uint32_t now, std::string& out);
  /** Call regularly; completes a code after a pause without data. */
  bool poll(uint32_t now, std::string& out);

 private:
  std::string buffer_;
  uint32_t lastByte_ = 0;
  bool take(std::string& out);
};

}  // namespace logic
