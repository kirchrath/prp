package home.inventory.recipe;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import home.inventory.recipe.common.dto.AuditDto;
import home.inventory.recipe.copy.CopyService;
import home.inventory.recipe.copy.dto.ArticleDataDto;
import home.inventory.recipe.copy.dto.ArticleEventDto;
import home.inventory.recipe.copy.dto.BatchDataDto;
import home.inventory.recipe.copy.dto.BatchEventDto;
import home.inventory.recipe.imports.PageFetcher;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests with PostgreSQL and Kafka; the AI provider is replaced by {@link FakeChatModel}. */
@SpringBootTest(properties = "spring.ai.model.chat=none")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, KafkaTestSupport.class, AbstractIntegrationTest.AiTestConfig.class})
public abstract class AbstractIntegrationTest {

    @TestConfiguration(proxyBeanMethods = false)
    static class AiTestConfig {
        @Bean
        FakeChatModel fakeChatModel() {
            return new FakeChatModel();
        }
    }

    protected static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice"));

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected KafkaTestSupport kafka;

    @Autowired
    protected FakeChatModel ai;

    @Autowired
    protected CopyService copies;

    /** Recipe pages are not loaded from the internet in integration tests (see PageFetcherTest). */
    @MockitoBean
    protected PageFetcher pages;

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(USER).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected UUID created(MockHttpServletRequestBuilder request) throws Exception {
        String location = mvc.perform(request).andReturn().getResponse().getHeader("Location");
        if (location == null) {
            throw new AssertionError("no Location header");
        }
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    protected <T> T read(String url, String path) throws Exception {
        return JsonPath.read(mvc.perform(get(url).with(USER)).andReturn().getResponse().getContentAsString(), path);
    }

    protected static String audit(long version) {
        return "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":"
                + version + ",\"deleted\":false}";
    }

    protected static AuditDto audit(long version, boolean deleted) {
        return new AuditDto(Instant.now(), Instant.now(), version, deleted);
    }

    /** Simulates catalog_article: article with package content. */
    protected UUID article(String name, String amount, String unit) {
        UUID id = UUID.randomUUID();
        copies.applyArticle(new ArticleEventDto(id, audit(0, false),
                new ArticleDataDto(name, null, amount == null ? null : new BigDecimal(amount), unit)));
        return id;
    }

    /** Simulates stock_batch: {@code packages} packages of the article in stock. */
    protected void stock(UUID articleId, int packages) {
        copies.applyBatch(new BatchEventDto(UUID.randomUUID(), audit(0, false), new BatchDataDto(articleId, packages)));
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
