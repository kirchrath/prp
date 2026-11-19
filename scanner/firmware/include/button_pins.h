#pragma once
// Button box wiring, see docs/scanner-hardware.md (LED, buzzer and config button as in pins.h)

// up to 8 push buttons between GPIO and GND (internal pull-up, LOW = pressed); unused inputs stay open
constexpr int BUTTON_PINS[] = {13, 16, 17, 18, 19, 21, 22, 23};
constexpr int BUTTON_COUNT = sizeof(BUTTON_PINS) / sizeof(BUTTON_PINS[0]);
