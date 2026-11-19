package home.inventory.stock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.testcontainers.kafka.KafkaContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reads and writes topics in tests. */
@Component
public class KafkaTestSupport {

    private final KafkaContainer kafka;
    private final KafkaTemplate<String, String> template;
    private final ObjectMapper json;

    public KafkaTestSupport(KafkaContainer kafka, KafkaTemplate<String, String> template, ObjectMapper json) {
        this.kafka = kafka;
        this.template = template;
        this.json = json;
    }

    public void send(String topic, String key, Object payload) {
        template.send(topic, key, payload instanceof String s ? s : json.writeValueAsString(payload)).join();
    }

    /** All events with the given key on the topic, waiting until at least {@code count} arrived. */
    public List<JsonNode> await(String topic, String key, int count) {
        List<JsonNode> found = new ArrayList<>();
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> {
            found.clear();
            found.addAll(read(topic, key));
            return found.size() >= count;
        });
        return found;
    }

    private List<JsonNode> read(String topic, String key) {
        Properties props = new Properties();
        props.putAll(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName(),
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()));
        List<JsonNode> result = new ArrayList<>();
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            for (int i = 0; i < 5; i++) {
                for (ConsumerRecord<String, String> r : consumer.poll(Duration.ofMillis(500))) {
                    if (key.equals(r.key())) {
                        result.add(json.readTree(r.value()));
                    }
                }
            }
        }
        return result;
    }
}
