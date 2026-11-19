#include "ota_command.h"

#include <ArduinoJson.h>

namespace logic {

static bool isHex64(const std::string& s) {
  if (s.size() != 64) return false;
  for (char c : s) {
    if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
  }
  return true;
}

bool parseOtaCommand(const char* payload, size_t length, OtaCommand& out) {
  JsonDocument doc;
  if (deserializeJson(doc, payload, length)) return false;
  out.job = doc["job"] | "";
  out.version = doc["version"] | "";
  out.sha256 = doc["sha256"] | "";
  out.path = doc["path"] | "";
  out.size = doc["size"] | 0u;
  uint32_t port = doc["port"] | 0u;
  out.port = static_cast<uint16_t>(port);
  return !out.job.empty() && !out.version.empty() && isHex64(out.sha256) && out.path.rfind("/ota/", 0) == 0 &&
         out.size > 0 && port > 0 && port <= 65535;
}

std::string otaStateMessage(const std::string& job, const char* state, const std::string& error) {
  JsonDocument doc;
  doc["job"] = job;
  doc["state"] = state;
  if (!error.empty()) doc["error"] = error;
  std::string out;
  serializeJson(doc, out);
  return out;
}

std::string toHex(const uint8_t* data, size_t length) {
  static const char* digits = "0123456789abcdef";
  std::string out;
  out.reserve(length * 2);
  for (size_t i = 0; i < length; i++) {
    out += digits[data[i] >> 4];
    out += digits[data[i] & 0x0F];
  }
  return out;
}

}  // namespace logic
