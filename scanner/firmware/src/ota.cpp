#include "ota.h"

#include <HTTPClient.h>
#include <Update.h>
#include <WiFi.h>
#include <mbedtls/sha256.h>

#include "feedback.h"

static void report(PubSubClient& mqtt, const String& topic, const logic::OtaCommand& cmd, const char* state,
                   const std::string& error = "") {
  mqtt.publish(topic.c_str(), logic::otaStateMessage(cmd.job, state, error).c_str());
  mqtt.loop();
  Serial.printf("OTA %s %s %s\n", cmd.version.c_str(), state, error.c_str());
}

// mbedtls 3 (arduino-esp32 3.x) dropped the *_ret variants that mbedtls 2.28 (arduino-esp32 2.x) recommends
#if MBEDTLS_VERSION_NUMBER >= 0x03000000
#define SHA_START(ctx) mbedtls_sha256_starts(ctx, 0)
#define SHA_UPDATE(ctx, buf, len) mbedtls_sha256_update(ctx, buf, len)
#define SHA_FINISH(ctx, out) mbedtls_sha256_finish(ctx, out)
#else
#define SHA_START(ctx) mbedtls_sha256_starts_ret(ctx, 0)
#define SHA_UPDATE(ctx, buf, len) mbedtls_sha256_update_ret(ctx, buf, len)
#define SHA_FINISH(ctx, out) mbedtls_sha256_finish_ret(ctx, out)
#endif

void otaInstall(const logic::OtaCommand& cmd, const String& host, PubSubClient& mqtt, const String& stateTopic) {
  feedbackShow(logic::Signal::Waiting);
  report(mqtt, stateTopic, cmd, "downloading");

  WiFiClient client;
  HTTPClient http;
  String url = "http://" + host + ":" + String(cmd.port) + String(cmd.path.c_str());
  http.setTimeout(15000);
  if (!http.begin(client, url)) {
    report(mqtt, stateTopic, cmd, "failed", "Download-Adresse ungültig");
    return;
  }
  int status = http.GET();
  if (status != 200 || static_cast<uint32_t>(http.getSize()) != cmd.size) {
    report(mqtt, stateTopic, cmd, "failed", "Download fehlgeschlagen (HTTP " + std::to_string(status) + ")");
    http.end();
    return;
  }
  if (!Update.begin(cmd.size)) {
    report(mqtt, stateTopic, cmd, "failed", std::string("Kein Platz: ") + Update.errorString());
    http.end();
    return;
  }

  mbedtls_sha256_context sha;
  mbedtls_sha256_init(&sha);
  SHA_START(&sha);
  WiFiClient* stream = http.getStreamPtr();
  uint8_t buffer[1024];
  uint32_t written = 0;
  uint32_t lastData = millis();
  while (written < cmd.size && millis() - lastData < 15000) {
    size_t available = stream->available();
    if (available == 0) {
      delay(5);
      continue;
    }
    size_t n = stream->readBytes(buffer, min(available, sizeof(buffer)));
    SHA_UPDATE(&sha, buffer, n);
    if (Update.write(buffer, n) != n) break;
    written += n;
    lastData = millis();
    mqtt.loop();
  }
  uint8_t digest[32];
  SHA_FINISH(&sha, digest);
  mbedtls_sha256_free(&sha);
  http.end();

  if (written != cmd.size) {
    Update.abort();
    report(mqtt, stateTopic, cmd, "failed", "Download unvollständig");
    return;
  }
  if (logic::toHex(digest, sizeof(digest)) != cmd.sha256) {
    Update.abort();
    report(mqtt, stateTopic, cmd, "failed", "Prüfsumme falsch – Firmware nicht installiert");
    return;
  }
  if (!Update.end(true)) {
    report(mqtt, stateTopic, cmd, "failed", std::string("Installation fehlgeschlagen: ") + Update.errorString());
    return;
  }
  report(mqtt, stateTopic, cmd, "installed");
  delay(500);
  ESP.restart();
}
