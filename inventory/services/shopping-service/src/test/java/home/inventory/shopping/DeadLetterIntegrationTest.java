package home.inventory.shopping;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.shopping.common.dto.AuditDto;
import home.inventory.shopping.copy.ArticleCopyRepository;
import home.inventory.shopping.copy.dto.ArticleDataDto;
import home.inventory.shopping.copy.dto.ArticleEventDto;
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

    private static String header(ConsumerRecord<String, String> record, String name) {
        return new String(record.headers().lastHeader(name).value(), StandardCharsets.UTF_8);
    }

    @Test
    void unreadableMessagesGoToTheDeadLetterTopicRightAway() {
        String key = UUID.randomUUID().toString();
        kafka.send("recipe_shopping_request", key, "not json");
        ConsumerRecord<String, String> dead = kafka.awaitRecords("shopping_dead_letter", key, 1).getFirst();
        assertThat(dead.value()).isEqualTo("not json");
        assertThat(header(dead, KafkaHeaders.ORIGINAL_TOPIC)).isEqualTo("recipe_shopping_request");
        assertThat(header(dead, KafkaHeaders.EXCEPTION_CAUSE_FQCN)).contains("jackson");
    }

    @Test
    void messagesFromTheRetryTopicAreProcessedLikeTheOriginal() {
        UUID id = UUID.randomUUID();
        kafka.send("catalog_article_shopping_retry", id.toString(), new ArticleEventDto(id,
                new AuditDto(Instant.now(), Instant.now(), 1L, false), new ArticleDataDto("Zucker", null, 0)));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> copies.findByArticleId(id).isPresent());
        assertThat(copies.findByArticleId(id).orElseThrow().name()).isEqualTo("Zucker");
    }
}
