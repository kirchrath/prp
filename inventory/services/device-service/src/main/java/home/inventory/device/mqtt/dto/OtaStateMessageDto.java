package home.inventory.device.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** {@code <prefix>/<deviceId>/otastate}: {@code state} is downloading, installed or failed. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OtaStateMessageDto(@Nullable UUID job, @Nullable String state, @Nullable String error) {
}
