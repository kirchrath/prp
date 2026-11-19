package home.inventory.device.mqtt;

import home.inventory.device.mqtt.dto.ButtonMessageDto;
import home.inventory.device.mqtt.dto.ScanMessageDto;
import home.inventory.device.mqtt.dto.StatusMessageDto;
import home.inventory.device.scan.FeedbackService;
import home.inventory.device.scan.ScanService;
import home.inventory.device.scanner.ScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Dispatches MQTT messages of {@code <prefix>/<deviceId>/scan|status}. The device id in the topic is trustworthy:
 * the broker ACL only allows a scanner to publish below its own user name.
 */
@Service
public class MqttInboundService {

    private static final Logger log = LoggerFactory.getLogger(MqttInboundService.class);

    private final ScannerService scanners;
    private final ScanService scans;
    private final FeedbackService feedback;
    private final ObjectMapper json;

    public MqttInboundService(ScannerService scanners, ScanService scans, FeedbackService feedback, ObjectMapper json) {
        this.scanners = scanners;
        this.scans = scans;
        this.feedback = feedback;
        this.json = json;
    }

    public void handle(String topic, String payload) {
        String[] parts = topic.split("/");
        if (parts.length < 2) {
            log.debug("Ignoring topic {}", topic);
            return;
        }
        String deviceId = parts[parts.length - 2];
        String kind = parts[parts.length - 1];
        try {
            if ("status".equals(kind)) {
                scanners.handleStatus(deviceId, json.readValue(payload, StatusMessageDto.class));
            } else if ("scan".equals(kind)) {
                scans.handleScan(deviceId, json.readValue(payload, ScanMessageDto.class));
            } else if ("button".equals(kind)) {
                scans.handlePress(deviceId, json.readValue(payload, ButtonMessageDto.class));
            } else {
                log.debug("Ignoring topic {}", topic);
            }
        } catch (JacksonException e) {
            log.warn("Invalid message on {}: {}", topic, e.getMessage());
            if ("scan".equals(kind) || "button".equals(kind)) {
                feedback.send(deviceId, null, FeedbackService.ERROR, null, null, "Ungültige Nachricht");
            }
        }
    }
}
