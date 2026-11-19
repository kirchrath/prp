package home.inventory.device;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.awaitility.Awaitility;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;
import org.testcontainers.containers.GenericContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Plays the ESP32 scanner in tests. */
@Component
public class MqttTestClient {

    private final GenericContainer<?> mosquitto;
    private final ObjectMapper json;
    private final List<String[]> received = new CopyOnWriteArrayList<>();
    private MqttClient client;

    public MqttTestClient(GenericContainer<?> mosquitto, ObjectMapper json) {
        this.mosquitto = mosquitto;
        this.json = json;
    }

    private synchronized MqttClient client() throws Exception {
        if (client == null) {
            client = new MqttClient("tcp://" + mosquitto.getHost() + ":" + mosquitto.getMappedPort(1883),
                    "test-" + UUID.randomUUID(), new MemoryPersistence());
            client.connect();
            client.subscribe("inventory/scanner/+/feedback", (topic, message) ->
                    received.add(new String[]{topic, new String(message.getPayload())}));
        }
        return client;
    }

    public void publish(String topic, String payload) throws Exception {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setQos(1);
        client().publish(topic, message);
    }

    /** Feedback messages for a device, waiting until at least {@code count} arrived. */
    public List<JsonNode> feedback(String deviceId, int count) throws Exception {
        client();
        String topic = "inventory/scanner/" + deviceId + "/feedback";
        Awaitility.await().atMost(Duration.ofSeconds(20))
                .until(() -> received.stream().filter(r -> r[0].equals(topic)).count() >= count);
        return received.stream().filter(r -> r[0].equals(topic)).map(r -> json.readTree(r[1])).toList();
    }

    public long feedbackCount(String deviceId) {
        return received.stream().filter(r -> r[0].equals("inventory/scanner/" + deviceId + "/feedback")).count();
    }
}
