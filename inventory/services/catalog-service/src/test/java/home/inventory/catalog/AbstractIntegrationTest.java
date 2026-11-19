package home.inventory.catalog;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.jayway.jsonpath.JsonPath;
import home.inventory.catalog.article.ProductLookupService;
import java.util.UUID;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "inventory.kafka.retry.backoff=300ms")
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, KafkaTestSupport.class})
public abstract class AbstractIntegrationTest {

    protected static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice"));

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected KafkaTestSupport kafka;

    /** external product databases are replaced in integration tests (see ProductLookupServiceTest) */
    @MockitoBean
    protected ProductLookupService lookup;

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(USER).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    /** Id from the Location header of a 201 response. */
    protected UUID created(MockHttpServletRequestBuilder request) throws Exception {
        String location = mvc.perform(request).andReturn().getResponse().getHeader("Location");
        if (location == null) {
            throw new AssertionError("no Location header");
        }
        return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
    }

    protected long version(String url) throws Exception {
        String body = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url).with(USER))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.audit.version")).longValue();
    }

    /** Audit as a client sends it back on updates. */
    protected static String audit(long version) {
        return "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":"
                + version + ",\"deleted\":false}";
    }

    protected static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
