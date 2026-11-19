package home.inventory.device.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** {@code <prefix>/<deviceId>/scan} */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScanMessageDto(@Nullable String ean, @Nullable String direction, @Nullable Long seq,
                             @Nullable String bootId) {
}
