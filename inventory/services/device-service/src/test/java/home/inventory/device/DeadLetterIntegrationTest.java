package home.inventory.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

class DeadLetterIntegrationTest extends AbstractIntegrationTest {

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @Test
    void unreadableMessagesGoToTheDeadLetterTopicRightAway() {
        String key = UUID.randomUUID().toString();
        kafka.send("stock_booking", key, "not json");
        ConsumerRecord<String, String> dead = kafka.awaitRecords("device_dead_letter", key, 1).getFirst();
        assertThat(dead.value()).isEqualTo("not json");
        assertThat(header(dead, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("stock_booking");
        assertThat(header(dead, KafkaHeaders.EXCEPTION_CAUSE_FQCN)).contains("jackson");
    }
}
