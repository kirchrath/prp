package home.inventory.device.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

/** {@code <prefix>/<deviceId>/button}: a button of a button box was pressed (IN = short, OUT = long press). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ButtonMessageDto(@Nullable Integer button, @Nullable String direction, @Nullable Long seq,
                               @Nullable String bootId) {
}
