package home.inventory.device.scanner.dto;

import home.inventory.device.scanner.DeviceKind;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ScannerListDto(UUID id, String deviceId, @Nullable String name, @Nullable UUID locationId,
                             boolean online, @Nullable String firmwareVersion, @Nullable String ipAddress,
                             @Nullable Instant lastSeen, DeviceKind kind, List<ButtonDto> buttons) {
}
