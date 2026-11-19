package home.inventory.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import home.inventory.notification.common.dto.AuditDto;
import home.inventory.notification.copy.CopyService;
import home.inventory.notification.copy.dto.ArticleDataDto;
import home.inventory.notification.copy.dto.ArticleEventDto;
import home.inventory.notification.copy.dto.BatchDataDto;
import home.inventory.notification.copy.dto.BatchEventDto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests with PostgreSQL and Kafka; ntfy is replaced by {@link FakeNtfy}, the scheduler is off. */
@SpringBootTest(properties = {
        "inventory.kafka.retry.backoff=300ms",
        "inventory.notification.cron=-",
        "inventory.notification.ntfy-token=secret",
        "inventory.notification.app-url=https://vorrat.example/"})
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, KafkaTestSupport.class})
public abstract class AbstractIntegrationTest {

    protected static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice"));

    @DynamicPropertySource
    static void ntfy(DynamicPropertyRegistry registry) {
        registry.add("inventory.notification.ntfy-url", FakeNtfy::url);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected KafkaTestSupport kafka;

    @Autowired
    protected CopyService copies;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Every test starts with default settings, no copies, no digests and a working ntfy. */
    @BeforeEach
    void resetState() {
        jdbc.update("delete from batch_copy");
        jdbc.update("delete from article_copy");
        jdbc.update("delete from digest");
        jdbc.update("delete from category_threshold");
        jdbc.update("update notification_settings set enabled = true, send_time = '08:00', default_days = 3");
        FakeNtfy.reset();
    }

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(USER).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static AuditDto audit(long version, boolean deleted) {
        return new AuditDto(Instant.now(), Instant.now(), version, deleted);
    }

    protected UUID article(String name, String brand, UUID categoryId) {
        UUID id = UUID.randomUUID();
        article(id, 1, false, name, brand, categoryId);
        return id;
    }

    protected void article(UUID id, long version, boolean deleted, String name, String brand, UUID categoryId) {
        copies.applyArticle(new ArticleEventDto(id, audit(version, deleted), new ArticleDataDto(name, brand, categoryId)));
    }

    protected UUID batch(UUID articleId, LocalDate bestBefore, int quantity) {
        UUID id = UUID.randomUUID();
        batch(id, 1, false, articleId, bestBefore, quantity);
        return id;
    }

    protected void batch(UUID id, long version, boolean deleted, UUID articleId, LocalDate bestBefore, int quantity) {
        copies.applyBatch(new BatchEventDto(id, audit(version, deleted), new BatchDataDto(articleId, bestBefore, quantity)));
    }
}
