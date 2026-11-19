package home.inventory.device.ota.dto;

import home.inventory.device.ota.OtaState;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Event data; the download token is not published. */
public record OtaJobDataDto(UUID scannerId, String deviceId, UUID firmwareId, String targetVersion, Instant expiresAt,
                            OtaState state, @Nullable String error) {
}
