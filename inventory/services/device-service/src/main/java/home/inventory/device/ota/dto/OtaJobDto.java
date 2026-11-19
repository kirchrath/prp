package home.inventory.device.ota.dto;

import home.inventory.device.ota.OtaState;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Last firmware update of a device, as shown in the device list. */
public record OtaJobDto(UUID id, String targetVersion, OtaState state, @Nullable String error, Instant lastModifiedDate) {
}
