#pragma once

#include <string>

namespace logic {

/**
 * Formal check of a barcode, same rules as the server: only digits, EAN-8, UPC-A (12), EAN-13 or GTIN-14 with a
 * valid check digit; UPC-E (8 digits) when it is not a valid EAN-8. Invalid codes are not sent.
 */
bool isValidGtin(const std::string& code);

}  // namespace logic
