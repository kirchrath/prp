#pragma once
#include <cstddef>
#include <cstdint>
#include <string>
#include "direction.h"

namespace logic {

/** Payload of {@code <prefix>/<deviceId>/scan}. */
std::string scanMessage(const std::string& ean, Direction direction, uint32_t seq, const std::string& bootId);

/** Payload of {@code <prefix>/<deviceId>/button} (button box, buttons numbered from 1). */
std::string buttonMessage(int button, Direction direction, uint32_t seq, const std::string& bootId);

/** Payload of {@code <prefix>/<deviceId>/status}; kind is "scanner" or "buttons". */
std::string statusMessage(const char* firmware, const std::string& ip, int rssi, const std::string& bootId,
                          const char* kind);

struct Feedback {
  uint32_t seq = 0;
  std::string result;
  std::string article;
};

/** Parses {@code <prefix>/<deviceId>/feedback}; false for invalid JSON. */
bool parseFeedback(const char* payload, size_t length, Feedback& out);

}  // namespace logic
