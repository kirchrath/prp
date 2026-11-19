#include "gtin.h"

namespace logic {

namespace {

bool hasValidCheckDigit(const std::string& code) {
  int sum = 0;
  int weight = 3;
  for (int i = static_cast<int>(code.size()) - 2; i >= 0; i--) {
    sum += (code[i] - '0') * weight;
    weight = 4 - weight;
  }
  return (10 - sum % 10) % 10 == code.back() - '0';
}

bool isValidUpcE(const std::string& code) {
  char system = code[0];
  if (system != '0' && system != '1') return false;
  std::string d = code.substr(1, 6);
  char last = d[5];
  std::string body;
  if (last <= '2') {
    body = d.substr(0, 2) + last + "0000" + d.substr(2, 3);
  } else if (last == '3') {
    body = d.substr(0, 3) + "00000" + d.substr(3, 2);
  } else if (last == '4') {
    body = d.substr(0, 4) + "00000" + d[4];
  } else {
    body = d.substr(0, 5) + "0000" + last;
  }
  return hasValidCheckDigit(std::string(1, system) + body + code[7]);
}

}  // namespace

bool isValidGtin(const std::string& code) {
  if (code.empty()) return false;
  for (char c : code) {
    if (c < '0' || c > '9') return false;
  }
  switch (code.size()) {
    case 8:
      return hasValidCheckDigit(code) || isValidUpcE(code);
    case 12:
    case 13:
    case 14:
      return hasValidCheckDigit(code);
    default:
      return false;
  }
}

}  // namespace logic
