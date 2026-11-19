package home.inventory.stock;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import home.inventory.stock.article.ArticleCopyService;
import home.inventory.stock.article.dto.ArticleDataDto;
import home.inventory.stock.article.dto.ArticleEventDto;
import home.inventory.stock.common.dto.AuditDto;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, KafkaTestSupport.class})
public abstract class AbstractIntegrationTest {

    protected static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice"));

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected KafkaTestSupport kafka;

    @Autowired
    protected ArticleCopyService articleCopies;

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

    protected String body(String url) throws Exception {
        return mvc.perform(get(url).with(USER)).andReturn().getResponse().getContentAsString();
    }

    protected <T> T read(String url, String path) throws Exception {
        return JsonPath.read(body(url), path);
    }

    protected static String audit(long version) {
        return "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":"
                + version + ",\"deleted\":false}";
    }

    protected UUID location(String name, UUID parent) throws Exception {
        return created(json(post("/api/stock/locations"),
                "{\"data\":{\"name\":\"%s\",\"parentId\":%s}}".formatted(name, parent == null ? null : "\"" + parent + "\"")));
    }

    /** Simulates a catalog_article event. */
    protected void article(UUID id, long version, boolean deleted, String name, String status, UUID mergedInto,
                           String... eans) {
        articleCopies.apply(new ArticleEventDto(id, new AuditDto(Instant.now(), Instant.now(), version, deleted),
                new ArticleDataDto(name, "Marke", "img", status, List.of(eans), mergedInto)));
    }

    protected UUID article(String name, String... eans) {
        UUID id = UUID.randomUUID();
        article(id, 0, false, name, "ACTIVE", null, eans);
        return id;
    }

    protected void book(String direction, UUID article, UUID location, int quantity, String bestBefore) throws Exception {
        mvc.perform(json(post("/api/stock/" + direction), "{\"articleId\":\"%s\",\"locationId\":\"%s\",\"quantity\":%d,\"bestBefore\":%s}"
                        .formatted(article, location, quantity, bestBefore == null ? null : "\"" + bestBefore + "\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    protected static String ean() {
        String body = String.valueOf(400_000_000_000L + (long) (Math.random() * 99_999_999_999L));
        int sum = 0;
        for (int i = body.length() - 1, weight = 3; i >= 0; i--, weight = 4 - weight) {
            sum += (body.charAt(i) - '0') * weight;
        }
        return body + (10 - sum % 10) % 10; // valid EAN-13
    }
}
