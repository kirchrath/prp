package home.inventory.catalog.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.support.KafkaHeaders;

class DeadLetterHandlerTest {

    @Test
    void readsHeadersAndLogs() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>("catalog_dead_letter", 0, 3, "k", "x");
        record.headers().add(KafkaHeaders.ORIGINAL_TOPIC, "catalog_article".getBytes(StandardCharsets.UTF_8));
        assertThat(DeadLetterHandler.header(record, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("catalog_article");
        assertThat(DeadLetterHandler.header(record, KafkaHeaders.EXCEPTION_MESSAGE)).isEqualTo("-");
        new DeadLetterHandler().handle(record);
    }
}
