package home.inventory.notification;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.notification.common.dto.AuditDto;
import home.inventory.notification.copy.ArticleCopyRepository;
import home.inventory.notification.copy.BatchCopyRepository;
import home.inventory.notification.copy.dto.BatchDataDto;
import home.inventory.notification.copy.dto.BatchEventDto;
import home.inventory.notification.copy.dto.ArticleDataDto;
import home.inventory.notification.copy.dto.ArticleEventDto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;

class DeadLetterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArticleCopyRepository copies;

    @Autowired
    BatchCopyRepository batches;

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @Test
    void unreadableMessagesGoToTheDeadLetterTopicRightAway() {
        String key = UUID.randomUUID().toString();
        kafka.send("stock_batch", key, "not json");
        ConsumerRecord<String, String> dead = kafka.awaitRecords("notification_dead_letter", key, 1).getFirst();
        assertThat(dead.value()).isEqualTo("not json");
        assertThat(header(dead, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("stock_batch");
        assertThat(header(dead, KafkaHeaders.EXCEPTION_CAUSE_FQCN)).contains("jackson");
    }

    @Test
    void messagesFromTheRetryTopicAreProcessedLikeTheOriginal() {
        UUID id = UUID.randomUUID();
        kafka.send("catalog_article_notification_retry", id.toString(), new ArticleEventDto(id,
                new AuditDto(Instant.now(), Instant.now(), 1L, false), new ArticleDataDto("Zucker", null, null)));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> copies.findByArticleId(id).isPresent());
        assertThat(copies.findByArticleId(id).orElseThrow().name()).isEqualTo("Zucker");
    }

    @Test
    void batchesArriveViaKafka() {
        UUID id = UUID.randomUUID();
        kafka.send("stock_batch", id.toString(), new BatchEventDto(id,
                new AuditDto(Instant.now(), Instant.now(), 1L, false),
                new BatchDataDto(UUID.randomUUID(), java.time.LocalDate.now(), 2)));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> batches.findByBatchId(id).isPresent());
        assertThat(batches.findByBatchId(id).orElseThrow().quantity()).isEqualTo(2);
    }
}
