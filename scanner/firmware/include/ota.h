#pragma once

#include <Arduino.h>
#include <PubSubClient.h>

#include "ota_command.h"

/**
 * Downloads the image of an OTA command from http://<host>:<port><path>, writes it to the inactive OTA
 * partition and activates it only if size and SHA-256 match; then restarts. Progress and errors are published
 * on stateTopic. On any error the running firmware stays active.
 */
void otaInstall(const logic::OtaCommand& cmd, const String& host, PubSubClient& mqtt, const String& stateTopic);
