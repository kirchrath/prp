package home.inventory.stock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class LocationIntegrationTest extends AbstractIntegrationTest {

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/stock/locations")).andExpect(status().isUnauthorized());
    }

    @Test
    void treeWithPathsAndCycleProtection() throws Exception {
        String rootName = unique("Keller");
        UUID root = location(rootName, null);
        UUID shelf = location("Regal 1", root);
        UUID box = location("Kiste", shelf);

        mvc.perform(get("/api/stock/locations").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + box + "')].path").value(Matchers.contains(rootName + " / Regal 1 / Kiste")));
        mvc.perform(get("/api/stock/locations/" + shelf).with(USER))
                .andExpect(jsonPath("$.data.parentId").value(root.toString()))
                .andExpect(jsonPath("$.audit.version").value(0));

        // moving the root below its grandchild or itself is rejected
        mvc.perform(json(put("/api/stock/locations/" + root), "{\"audit\":%s,\"data\":{\"name\":\"K\",\"parentId\":\"%s\"}}".formatted(audit(0), box)))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/stock/locations/" + root), "{\"audit\":%s,\"data\":{\"name\":\"K\",\"parentId\":\"%s\"}}".formatted(audit(0), root)))
                .andExpect(status().isConflict());
        mvc.perform(json(put("/api/stock/locations/" + root), "{\"audit\":%s,\"data\":{\"name\":\"K\",\"parentId\":\"%s\"}}".formatted(audit(0), UUID.randomUUID())))
                .andExpect(status().isNotFound());
        mvc.perform(json(put("/api/stock/locations/" + box), "{\"data\":{\"name\":\"K\"}}")).andExpect(status().isConflict());
        mvc.perform(json(put("/api/stock/locations/" + box), "{\"audit\":%s,\"data\":{\"name\":\" Kiste 2 \",\"description\":\" oben \",\"parentId\":\"%s\"}}"
                        .formatted(audit(0), root)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/stock/locations/" + box).with(USER))
                .andExpect(jsonPath("$.data.name").value("Kiste 2"))
                .andExpect(jsonPath("$.data.description").value("oben"));

        // a location with children cannot be deleted
        mvc.perform(delete("/api/stock/locations/" + root).param("version", "0").with(USER)).andExpect(status().isConflict());
        mvc.perform(delete("/api/stock/locations/" + box).param("version", "1").with(USER)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/stock/locations/" + shelf).param("version", "0").with(USER)).andExpect(status().isNoContent());
        mvc.perform(get("/api/stock/locations").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + box + "')]").isEmpty());
        mvc.perform(get("/api/stock/locations/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());

        assertThat(kafka.await("stock_location", box.toString(), 3).getLast().get("audit").get("deleted").asBoolean()).isTrue();
    }

    @Test
    void locationWithStockCannotBeDeleted() throws Exception {
        UUID location = location(unique("Bad"), null);
        UUID article = article("Seife", ean());
        book("in", article, location, 1, null);
        mvc.perform(delete("/api/stock/locations/" + location).param("version", "0").with(USER)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/stock/locations"), "{\"data\":{\"name\":\"\"}}")).andExpect(status().isBadRequest());
        UUID blank = created(json(post("/api/stock/locations"), "{\"data\":{\"name\":\"Flur\",\"description\":\"  \"}}"));
        mvc.perform(get("/api/stock/locations/" + blank).with(USER))
                .andExpect(jsonPath("$.data.description").doesNotExist());
    }
}
