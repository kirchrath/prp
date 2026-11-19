package home.inventory.device.scan;

import home.inventory.device.config.MqttProperties;
import home.inventory.device.mqtt.MqttPublisher;
import home.inventory.device.mqtt.dto.FeedbackMessageDto;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Sends the result of a scan to the scanner (LED/buzzer). */
@Service
public class FeedbackService {

    public static final String ERROR = "ERROR";
    public static final String NO_LOCATION = "NO_LOCATION";
    public static final String INVALID_EAN = "INVALID_EAN";
    public static final String NO_ARTICLE = "NO_ARTICLE";
    private static final Set<String> SUCCESS = Set.of("IN_OK", "IN_OK_NEW_ARTICLE", "IN_OK_UNKNOWN_ARTICLE", "OUT_OK");

    private final MqttPublisher mqtt;
    private final MqttProperties properties;
    private final ObjectMapper json;

    public FeedbackService(MqttPublisher mqtt, MqttProperties properties, ObjectMapper json) {
        this.mqtt = mqtt;
        this.properties = properties;
        this.json = json;
    }

    public void send(String deviceId, @Nullable Long seq, String result, @Nullable String article,
                     @Nullable Long quantity, String message) {
        FeedbackMessageDto feedback = new FeedbackMessageDto(seq, result, SUCCESS.contains(result), article, quantity, message);
        mqtt.publish(properties.topicPrefix() + "/" + deviceId + "/feedback", json.writeValueAsString(feedback));
    }
}
