package home.inventory.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

class CategoryIntegrationTest extends AbstractIntegrationTest {

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/catalog/categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void createUpdateAndSoftDelete() throws Exception {
        String name = unique("Putzmittel");
        UUID id = created(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\" %s \"}}".formatted(name)));

        mvc.perform(get("/api/catalog/categories/" + id).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.audit.version").value(0))
                .andExpect(jsonPath("$.audit.deleted").value(false))
                .andExpect(jsonPath("$.audit.createdDate").exists())
                .andExpect(jsonPath("$.audit.lastModifiedDate").exists())
                .andExpect(jsonPath("$.id").doesNotExist());

        mvc.perform(json(put("/api/catalog/categories/" + id), "{\"audit\":" + audit(0) + ",\"data\":{\"name\":\"%s-neu\"}}".formatted(name)))
                .andExpect(status().isNoContent());
        // stale version
        mvc.perform(json(put("/api/catalog/categories/" + id), "{\"audit\":" + audit(0) + ",\"data\":{\"name\":\"x\"}}"))
                .andExpect(status().isConflict());
        // update without audit
        mvc.perform(json(put("/api/catalog/categories/" + id), "{\"data\":{\"name\":\"x\"}}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/catalog/categories").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].name").value(Matchers.contains(name + "-neu")));

        mvc.perform(delete("/api/catalog/categories/" + id).param("version", "0").with(USER)).andExpect(status().isConflict());
        mvc.perform(delete("/api/catalog/categories/" + id).param("version", "1").with(USER)).andExpect(status().isNoContent());

        mvc.perform(get("/api/catalog/categories").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").isEmpty());
        mvc.perform(get("/api/catalog/categories/" + id).with(USER))
                .andExpect(jsonPath("$.audit.deleted").value(true));

        List<JsonNode> events = kafka.await("catalog_category", id.toString(), 3);
        JsonNode last = events.getLast();
        assertThat(last.get("id").asString()).isEqualTo(id.toString());
        assertThat(last.get("audit").get("deleted").asBoolean()).isTrue();
        assertThat(last.get("audit").get("version").asLong()).isEqualTo(2);
        assertThat(last.get("data").get("name").asString()).isEqualTo(name + "-neu");
    }

    @Test
    void namesAreUniqueAmongActiveCategories() throws Exception {
        String name = unique("Lebensmittel");
        UUID first = created(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\"%s\"}}".formatted(name)));
        mvc.perform(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\"%s\"}}".formatted(name.toUpperCase())))
                .andExpect(status().isConflict());
        // renaming to its own name is fine
        mvc.perform(json(put("/api/catalog/categories/" + first), "{\"audit\":" + audit(0) + ",\"data\":{\"name\":\"%s\"}}".formatted(name)))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/catalog/categories/" + first).param("version", "1").with(USER)).andExpect(status().isNoContent());
        // after deleting, the name is free again
        mvc.perform(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\"%s\"}}".formatted(name)))
                .andExpect(status().isCreated());
    }

    @Test
    void unknownCategoryIsNotFound() throws Exception {
        mvc.perform(get("/api/catalog/categories/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());
    }

    @Test
    void validatesInput() throws Exception {
        mvc.perform(json(post("/api/catalog/categories"), "{\"data\":{\"name\":\"\"}}")).andExpect(status().isBadRequest());
    }
}
