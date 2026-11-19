package home.inventory.device.scanner.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Scanner data; only {@code name} and {@code locationId} can be changed, the rest is reported by the device. */
public record ScannerDataDto(@Nullable String deviceId, @Nullable @Size(max = 100) String name,
                             @Nullable UUID locationId, boolean online, @Nullable String firmwareVersion,
                             @Nullable String ipAddress, @Nullable Instant lastSeen) {
}
