package home.inventory.device.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** {@code <prefix>/<deviceId>/status} (retained, last will {@code {"online":false}}) */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StatusMessageDto(boolean online, @Nullable String fw, @Nullable String ip, @Nullable Integer rssi,
                               @Nullable String bootId) {
}
