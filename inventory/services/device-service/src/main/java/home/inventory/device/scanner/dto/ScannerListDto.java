package home.inventory.device.scanner.dto;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ScannerListDto(UUID id, String deviceId, @Nullable String name, @Nullable UUID locationId,
                             boolean online, @Nullable String firmwareVersion, @Nullable String ipAddress,
                             @Nullable Instant lastSeen) {
}
