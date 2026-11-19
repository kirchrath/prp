package home.inventory.shopping.common;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/** Messages that failed all attempts stay in the dead letter topic and are logged; they are not processed again. */
@Component
public class DeadLetterHandler {

    private static final Logger log = LoggerFactory.getLogger(DeadLetterHandler.class);

    public void handle(ConsumerRecord<String, String> record) {
        log.error("Nachricht nicht verarbeitet, abgelegt in {} (Offset {}): Topic {}, Key {}, Fehler {}: {}",
                record.topic(), record.offset(), header(record, KafkaHeaders.ORIGINAL_TOPIC), record.key(),
                header(record, KafkaHeaders.EXCEPTION_CAUSE_FQCN), header(record, KafkaHeaders.EXCEPTION_MESSAGE));
    }

    static String header(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);
        return header == null ? "-" : new String(header.value(), StandardCharsets.UTF_8);
    }
}
