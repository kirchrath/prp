package home.inventory.device.scan.dto;

import home.inventory.device.scan.ScanStatus;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record ScanDataDto(UUID scannerId, String deviceId, String ean, String direction, @Nullable UUID locationId,
                          long seq, String bootId, ScanStatus status, @Nullable String result,
                          @Nullable String articleName, @Nullable Long quantity, @Nullable String message,
                          @Nullable UUID bookingId) {
}
