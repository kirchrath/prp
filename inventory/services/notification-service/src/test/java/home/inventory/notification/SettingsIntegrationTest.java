package home.inventory.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class SettingsIntegrationTest extends AbstractIntegrationTest {

    private long version() throws Exception {
        Number v = JsonPath.read(mvc.perform(get("/api/notification/settings").with(USER))
                .andReturn().getResponse().getContentAsString(), "$.audit.version");
        return v.longValue();
    }

    private String body(long version, String data) {
        return "{\"audit\":{\"createdDate\":\"2026-01-01T00:00:00Z\",\"lastModifiedDate\":\"2026-01-01T00:00:00Z\","
                + "\"version\":" + version + ",\"deleted\":false},\"data\":" + data + "}";
    }

    @Test
    void defaultsAndUpdate() throws Exception {
        mvc.perform(get("/api/notification/settings").with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.sendTime").value("08:00:00"))
                .andExpect(jsonPath("$.data.defaultDays").value(3))
                .andExpect(jsonPath("$.data.categoryThresholds").isEmpty());

        UUID dairy = UUID.randomUUID();
        UUID cans = UUID.randomUUID();
        long version = version();
        mvc.perform(json(put("/api/notification/settings"), body(version, """
                        {"enabled":false,"sendTime":"18:30:45","defaultDays":5,
                         "categoryThresholds":[{"categoryId":"%s","days":1},{"categoryId":"%s","days":30}]}"""
                        .formatted(dairy, cans))))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/notification/settings").with(USER))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.sendTime").value("18:30:00"))
                .andExpect(jsonPath("$.data.defaultDays").value(5))
                .andExpect(jsonPath("$.data.categoryThresholds[*].days").value(Matchers.containsInAnyOrder(1, 30)));

        JsonNode event = kafka.await("notification_settings", "00000000-0000-0000-0000-000000000001", 1).getLast();
        assertThat(event.get("data").get("defaultDays").asInt()).isEqualTo(5);

        // outdated version
        mvc.perform(json(put("/api/notification/settings"), body(version, """
                        {"enabled":true,"sendTime":"08:00","defaultDays":3,"categoryThresholds":[]}""")))
                .andExpect(status().isConflict());
        // one threshold per category
        mvc.perform(json(put("/api/notification/settings"), body(version(), """
                        {"enabled":true,"sendTime":"08:00","defaultDays":3,
                         "categoryThresholds":[{"categoryId":"%s","days":1},{"categoryId":"%s","days":2}]}"""
                        .formatted(dairy, dairy))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("Kategorie")));
        // validation
        mvc.perform(json(put("/api/notification/settings"), body(version(), """
                        {"enabled":true,"sendTime":"08:00","defaultDays":400,"categoryThresholds":[]}""")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMessage() throws Exception {
        mvc.perform(post("/api/notification/settings/test").with(USER)).andExpect(status().isNoContent());
        FakeNtfy.Request request = FakeNtfy.requests().getLast();
        assertThat(request.authorization()).isEqualTo("Bearer secret");
        assertThat(request.body()).contains("\"topic\":\"vorrat\"", "Testnachricht",
                "\"click\":\"https://vorrat.example/benachrichtigungen\"");

        FakeNtfy.failWith(500);
        mvc.perform(post("/api/notification/settings/test").with(USER))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(Matchers.startsWith("ntfy:")));
    }
}
