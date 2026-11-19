package home.inventory.shopping;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import home.inventory.shopping.common.dto.AuditDto;
import home.inventory.shopping.copy.CopyService;
import home.inventory.shopping.copy.dto.ArticleDataDto;
import home.inventory.shopping.copy.dto.ArticleEventDto;
import home.inventory.shopping.copy.dto.BatchDataDto;
import home.inventory.shopping.copy.dto.BatchEventDto;
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
    protected CopyService copies;

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

    protected static String audit(long version) {
        return "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":"
                + version + ",\"deleted\":false}";
    }

    protected static AuditDto audit(long version, boolean deleted) {
        return new AuditDto(Instant.now(), Instant.now(), version, deleted);
    }

    protected void article(UUID id, long version, boolean deleted, String name, int minStock) {
        copies.applyArticle(new ArticleEventDto(id, audit(version, deleted), new ArticleDataDto(name, "Marke", minStock)));
    }

    protected void batch(UUID id, long version, boolean deleted, UUID articleId, int quantity) {
        copies.applyBatch(new BatchEventDto(id, audit(version, deleted), new BatchDataDto(articleId, quantity)));
    }

    /** Automatic entries of an article: list of quantities. */
    protected List<Integer> automatic(UUID articleId) throws Exception {
        String body = mvc.perform(get("/api/shopping/items").with(USER)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[?(@.articleId=='" + articleId + "' && @.automatic==true)].quantity");
    }
}
