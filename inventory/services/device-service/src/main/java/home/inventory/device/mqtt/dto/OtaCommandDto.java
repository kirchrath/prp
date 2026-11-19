package home.inventory.device.mqtt.dto;

import java.util.UUID;

/**
 * {@code <prefix>/<deviceId>/ota}: install the image at {@code http://<broker host>:<port><path>}, but only if its
 * SHA-256 matches. The device reports progress on {@code …/otastate}.
 */
public record OtaCommandDto(UUID job, String version, long size, String sha256, int port, String path) {
}
