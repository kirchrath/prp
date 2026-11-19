package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.stock.article.ArticleCopyRepository;
import home.inventory.stock.article.dto.ArticleDataDto;
import home.inventory.stock.article.dto.ArticleEventDto;
import home.inventory.stock.common.dto.AuditDto;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.support.KafkaHeaders;

class DeadLetterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArticleCopyRepository copies;

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @Test
    void unreadableMessagesGoToTheDeadLetterTopicRightAway() {
        String key = UUID.randomUUID().toString();
        kafka.send("device_scan", key, "not json");
        ConsumerRecord<String, String> dead = kafka.awaitRecords("stock_dead_letter", key, 1).getFirst();
        assertThat(dead.value()).isEqualTo("not json");
        assertThat(header(dead, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("device_scan");
        assertThat(header(dead, KafkaHeaders.EXCEPTION_CAUSE_FQCN)).contains("jackson");
    }

    @Test
    void failingMessagesAreRetriedAndThenDeadLettered() {
        String key = UUID.randomUUID().toString();
        // valid JSON, but the event has no data: fails on every attempt
        String payload = "{\"id\":\"%s\",\"audit\":%s}".formatted(key, audit(1));
        kafka.send("catalog_article", key, payload);
        assertThat(kafka.awaitRecords("catalog_article_stock_retry", key, 1)).isNotEmpty();
        ConsumerRecord<String, String> dead = kafka.awaitRecords("stock_dead_letter", key, 1).getFirst();
        assertThat(dead.value()).isEqualTo(payload);
        assertThat(header(dead, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("catalog_article");
        assertThat(header(dead, KafkaHeaders.EXCEPTION_CAUSE_FQCN)).isEqualTo("java.lang.NullPointerException");
    }

    @Test
    void messagesFromTheRetryTopicAreProcessedLikeTheOriginal() {
        UUID id = UUID.randomUUID();
        kafka.send("catalog_article_stock_retry", id.toString(), new ArticleEventDto(id,
                new AuditDto(Instant.now(), Instant.now(), 1L, false),
                new ArticleDataDto("Zucker", null, null, "ACTIVE", List.of(), null)));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> copies.findByArticleId(id).isPresent());
        assertThat(copies.findByArticleId(id).orElseThrow().name()).isEqualTo("Zucker");
    }
}
