#pragma once

#include <cstddef>
#include <cstdint>
#include <string>

namespace logic {

/** {@code <prefix>/<deviceId>/ota}: install firmware from http://<broker host>:<port><path>. */
struct OtaCommand {
  std::string job;
  std::string version;
  std::string sha256;  // 64 hex digits, lower case
  std::string path;    // /ota/<token>
  uint32_t size = 0;
  uint16_t port = 0;
};

/** Parses and validates the command; false for invalid JSON or missing/implausible fields. */
bool parseOtaCommand(const char* payload, size_t length, OtaCommand& out);

/** Payload of {@code …/otastate}: state is "downloading", "installed" or "failed". */
std::string otaStateMessage(const std::string& job, const char* state, const std::string& error);

/** Lower case hex of a digest (for comparing the SHA-256). */
std::string toHex(const uint8_t* data, size_t length);

}  // namespace logic
