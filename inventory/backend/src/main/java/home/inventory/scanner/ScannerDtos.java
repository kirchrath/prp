package home.inventory.scanner;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public final class ScannerDtos {

    private ScannerDtos() {
    }

    public record ScannerDto(Long id, String deviceId, @Nullable String name, @Nullable Long locationId,
                             @Nullable String locationPath, boolean online, @Nullable String firmwareVersion,
                             @Nullable String ipAddress, @Nullable Instant lastSeen) {
        static ScannerDto of(Scanner s) {
            var l = s.getLocation();
            return new ScannerDto(s.getId(), s.getDeviceId(), s.getName(), l == null ? null : l.getId(),
                    l == null ? null : l.getPath(), s.isOnline(), s.getFirmwareVersion(), s.getIpAddress(),
                    s.getLastSeen());
        }
    }

    public record ScannerUpdateRequest(@Nullable @Size(max = 100) String name, @Nullable Long locationId) {
    }
}
