package home.inventory.scanner;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/** JSON payloads exchanged with the ESP32 scanners via MQTT (see docs/mqtt.md). */
public final class ScannerMessages {

    private ScannerMessages() {
    }

    /** {@code <prefix>/<deviceId>/scan} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ScanMessage(@Nullable String ean, @Nullable String direction, @Nullable Long seq,
                              @Nullable String bootId) {
    }

    /** {@code <prefix>/<deviceId>/status} (retained, last will: {@code {"online":false}}) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StatusMessage(boolean online, @Nullable String fw, @Nullable String ip, @Nullable Integer rssi,
                                @Nullable String bootId) {
    }

    /** {@code <prefix>/<deviceId>/feedback} */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record FeedbackMessage(@Nullable Long seq, String result, boolean success, @Nullable String article,
                                  @Nullable Long quantity, String message) {
    }
}
