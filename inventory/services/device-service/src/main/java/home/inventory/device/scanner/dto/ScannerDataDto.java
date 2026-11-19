package home.inventory.device.scanner.dto;

import home.inventory.device.scanner.DeviceKind;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Device data; {@code name}, {@code locationId} and (button boxes) {@code buttons} can be changed, the rest is
 * reported by the device.
 */
public record ScannerDataDto(@Nullable String deviceId, @Nullable @Size(max = 100) String name,
                             @Nullable UUID locationId, boolean online, @Nullable String firmwareVersion,
                             @Nullable String ipAddress, @Nullable Instant lastSeen, @Nullable DeviceKind kind,
                             @NotNull List<@NotNull @Valid ButtonDto> buttons) {
}
