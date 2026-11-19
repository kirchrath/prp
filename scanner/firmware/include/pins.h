#pragma once
// Wiring, see docs/scanner-hardware.md

// GM65 barcode module (UART2)
constexpr int PIN_SCANNER_RX = 16;  // ESP32 RX  <- GM65 TX
constexpr int PIN_SCANNER_TX = 17;  // ESP32 TX  -> GM65 RX
constexpr uint32_t SCANNER_BAUD = 9600;

// IR light barriers / reflective sensors, LOW = beam interrupted / object detected.
// Sensor A sits on the "outside" (room side), sensor B on the "inside" (shelf side):
// A then B = item moves in, B then A = item moves out.
constexpr int PIN_SENSOR_A = 32;
constexpr int PIN_SENSOR_B = 33;

// RGB LED, common cathode (via 220 Ohm resistors)
constexpr int PIN_LED_R = 25;
constexpr int PIN_LED_G = 26;
constexpr int PIN_LED_B = 27;

// passive buzzer
constexpr int PIN_BUZZER = 14;

// BOOT button: hold 5 s to clear WiFi/MQTT settings and open the config portal
constexpr int PIN_CONFIG_BUTTON = 0;
