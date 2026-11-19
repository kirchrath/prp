package home.inventory.scanner;

import home.inventory.config.InventoryProperties;
import home.inventory.scanner.ScannerMessages.FeedbackMessage;
import home.inventory.scanner.ScannerMessages.ScanMessage;
import home.inventory.scanner.ScannerMessages.StatusMessage;
import home.inventory.stock.ScanOutcome;
import home.inventory.stock.ScanResult;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Bridges the ESP32 scanners (MQTT) to the {@link ScannerService}.
 * <ul>
 *   <li>subscribes {@code <prefix>/+/scan} and {@code <prefix>/+/status}</li>
 *   <li>answers every scan on {@code <prefix>/<deviceId>/feedback}</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "inventory.mqtt.enabled", havingValue = "true")
public class MqttScannerGateway implements SmartLifecycle, MqttCallbackExtended {

    private static final Logger log = LoggerFactory.getLogger(MqttScannerGateway.class);

    private final InventoryProperties.Mqtt properties;
    private final ScannerService scanners;
    private final ObjectMapper json;
    /** Connection handling (retry loop) runs here. */
    private final ExecutorService connector = Executors.newVirtualThreadPerTaskExecutor();
    /**
     * Messages are processed strictly one after another in arrival order: a scan must see the result of the
     * previous one (e.g. the article created by the first scan of a new barcode).
     */
    private final ExecutorService worker = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("mqtt-worker").factory());
    private @Nullable MqttClient client;
    private volatile boolean running;

    MqttScannerGateway(InventoryProperties properties, ScannerService scanners, ObjectMapper json) {
        this.properties = properties.mqtt();
        this.scanners = scanners;
        this.json = json;
    }

    @Override
    public void start() {
        try {
            MqttClient c = new MqttClient(properties.url(), properties.clientId(), new MemoryPersistence());
            c.setCallback(this);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(30);
            if (!properties.username().isBlank()) {
                options.setUserName(properties.username());
                options.setPassword(properties.password().toCharArray());
            }
            client = c;
            running = true;
            // connect in background so the application also starts while the broker is down
            connector.submit(() -> connectWithRetry(c, options));
        } catch (MqttException e) {
            throw new IllegalStateException("Cannot create MQTT client for " + properties.url(), e);
        }
    }

    private void connectWithRetry(MqttClient c, MqttConnectOptions options) {
        long delay = 1000;
        while (running && !c.isConnected()) {
            try {
                c.connect(options);
            } catch (MqttException e) {
                log.warn("MQTT connect to {} failed: {} – retrying in {} ms", properties.url(), e.getMessage(), delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delay = Math.min(delay * 2, 30_000);
            }
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        log.info("MQTT connected to {} (reconnect={})", serverURI, reconnect);
        try {
            String prefix = properties.topicPrefix();
            client.subscribe(new String[]{prefix + "/+/scan", prefix + "/+/status"}, new int[]{1, 1});
        } catch (MqttException e) {
            log.error("MQTT subscribe failed", e);
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause.getMessage());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        // never block the Paho callback thread (lookups may call external HTTP services)
        byte[] payload = message.getPayload();
        worker.submit(() -> dispatch(topic, payload));
    }

    void dispatch(String topic, byte[] payload) {
        String[] parts = topic.split("/");
        if (parts.length < 2) {
            return;
        }
        String deviceId = parts[parts.length - 2];
        String kind = parts[parts.length - 1];
        try {
            switch (kind) {
                case "status" -> scanners.handleStatus(deviceId, json.readValue(payload, StatusMessage.class));
                case "scan" -> handleScan(deviceId, payload);
                default -> log.debug("Ignoring topic {}", topic);
            }
        } catch (RuntimeException e) {
            log.error("Failed to process {}: {}", topic, e.getMessage(), e);
        }
    }

    private void handleScan(String deviceId, byte[] payload) {
        ScanMessage scan;
        try {
            scan = json.readValue(payload, ScanMessage.class);
        } catch (RuntimeException e) {
            publishFeedback(deviceId, new FeedbackMessage(null, ScanOutcome.ERROR.name(), false, null, null,
                    "Ungültige Scan-Nachricht"));
            return;
        }
        try {
            scanners.handleScan(deviceId, scan).ifPresent(result -> publishFeedback(deviceId, feedback(scan, result)));
        } catch (RuntimeException e) {
            log.error("Scan of {} by {} failed", scan.ean(), deviceId, e);
            publishFeedback(deviceId, new FeedbackMessage(scan.seq(), ScanOutcome.ERROR.name(), false, null, null,
                    "Interner Fehler"));
        }
    }

    private static FeedbackMessage feedback(ScanMessage scan, ScanResult result) {
        return new FeedbackMessage(scan.seq(), result.outcome().name(), result.outcome().isSuccess(),
                result.articleName(), result.quantityAtLocation(), result.message());
    }

    private void publishFeedback(String deviceId, FeedbackMessage feedback) {
        MqttClient c = client;
        if (c == null || !c.isConnected()) {
            return;
        }
        try {
            MqttMessage msg = new MqttMessage(json.writeValueAsBytes(feedback));
            msg.setQos(1);
            c.publish(properties.topicPrefix() + "/" + deviceId + "/feedback", msg);
        } catch (MqttException e) {
            log.warn("Could not publish feedback to {}: {}", deviceId, e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // nothing to do
    }

    @Override
    public void stop() {
        running = false;
        MqttClient c = client;
        if (c != null) {
            try {
                if (c.isConnected()) {
                    c.disconnect();
                }
                c.close();
            } catch (MqttException e) {
                log.debug("Error while closing MQTT client", e);
            }
        }
        connector.shutdownNow();
        worker.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
