#include "feedback.h"
#include "pins.h"

using logic::Signal;

static Signal current = Signal::Idle;
static uint32_t shownAt = 0;
static const uint32_t SHOW_MS = 1500;

static void setRgb(uint8_t r, uint8_t g, uint8_t b) {
  analogWrite(PIN_LED_R, r);
  analogWrite(PIN_LED_G, g);
  analogWrite(PIN_LED_B, b);
}

static void beep(uint16_t freq, uint16_t ms, uint8_t count = 1) {
  for (uint8_t i = 0; i < count; i++) {
    tone(PIN_BUZZER, freq, ms);
    delay(ms + 60);
  }
}

void feedbackBegin() {
  pinMode(PIN_LED_R, OUTPUT);
  pinMode(PIN_LED_G, OUTPUT);
  pinMode(PIN_LED_B, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  setRgb(0, 0, 0);
}

void feedbackShow(Signal s) {
  current = s;
  shownAt = millis();
  switch (s) {
    case Signal::Idle:        setRgb(0, 0, 0); break;
    case Signal::Waiting:     setRgb(40, 40, 40); break;
    case Signal::InOk:        setRgb(0, 255, 0); beep(2400, 80); break;
    case Signal::InUnknown:   setRgb(255, 160, 0); beep(2400, 80, 2); break;
    case Signal::OutOk:       setRgb(0, 0, 255); beep(1600, 80); break;
    case Signal::Error:       setRgb(255, 0, 0); beep(400, 500); break;
    case Signal::NoDirection: setRgb(255, 0, 255); beep(600, 250); break;
    case Signal::Queued:      setRgb(0, 200, 200); beep(1800, 60); break;
    case Signal::Offline:     break;
  }
}

void feedbackLoop(bool online) {
  uint32_t now = millis();
  bool timed = current != Signal::Idle && current != Signal::Offline && current != Signal::Waiting;
  if (timed && now - shownAt > SHOW_MS) {
    current = Signal::Idle;
    setRgb(0, 0, 0);
  }
  if (current == Signal::Idle || current == Signal::Offline) {
    if (!online) {
      current = Signal::Offline;
      bool on = (now / 1000) % 2 == 0;
      setRgb(on ? 120 : 0, on ? 40 : 0, 0);
    } else if (current == Signal::Offline) {
      current = Signal::Idle;
      setRgb(0, 0, 0);
    }
  }
}
