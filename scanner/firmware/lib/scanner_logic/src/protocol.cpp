#include "protocol.h"
#include <ArduinoJson.h>

namespace logic {

std::string scanMessage(const std::string& ean, Direction direction, uint32_t seq, const std::string& bootId) {
  JsonDocument doc;
  doc["ean"] = ean;
  doc["direction"] = direction == Direction::In ? "IN" : "OUT";
  doc["seq"] = seq;
  doc["bootId"] = bootId;
  std::string out;
  serializeJson(doc, out);
  return out;
}

std::string buttonMessage(int button, Direction direction, uint32_t seq, const std::string& bootId) {
  JsonDocument doc;
  doc["button"] = button;
  doc["direction"] = direction == Direction::In ? "IN" : "OUT";
  doc["seq"] = seq;
  doc["bootId"] = bootId;
  std::string out;
  serializeJson(doc, out);
  return out;
}

std::string statusMessage(const char* firmware, const std::string& ip, int rssi, const std::string& bootId,
                          const char* kind) {
  JsonDocument doc;
  doc["online"] = true;
  doc["fw"] = firmware;
  doc["ip"] = ip;
  doc["rssi"] = rssi;
  doc["bootId"] = bootId;
  doc["kind"] = kind;
  std::string out;
  serializeJson(doc, out);
  return out;
}

bool parseFeedback(const char* payload, size_t length, Feedback& out) {
  JsonDocument doc;
  if (deserializeJson(doc, payload, length)) return false;
  out.seq = doc["seq"] | 0u;
  out.result = doc["result"] | "ERROR";
  out.article = doc["article"] | "";
  return true;
}

}  // namespace logic
