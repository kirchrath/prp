package home.inventory.device;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.util.UUID;
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
@Import({TestcontainersConfiguration.class, KafkaTestSupport.class, MqttTestClient.class})
public abstract class AbstractIntegrationTest {

    protected static final RequestPostProcessor USER = jwt().jwt(j -> j.subject("alice"));

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected KafkaTestSupport kafka;

    @Autowired
    protected MqttTestClient mqtt;

    protected MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder, String body) {
        return builder.with(USER).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    protected static String audit(long version) {
        return "{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\",\"version\":"
                + version + ",\"deleted\":false}";
    }

    protected static String device() {
        return "scanner-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
