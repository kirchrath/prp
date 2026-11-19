package home.inventory.device.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** {@code <prefix>/<deviceId>/feedback}; {@code PENDING_ARTICLE} means "keep waiting". */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedbackMessageDto(@Nullable Long seq, String result, boolean success, @Nullable String article,
                                 @Nullable Long quantity, String message) {
}
