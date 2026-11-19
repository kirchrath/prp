package home.inventory.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import home.inventory.shopping.copy.ArticleCopyRepository;
import home.inventory.shopping.item.ShoppingItemService;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ShoppingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ShoppingItemService service;

    @Autowired
    ArticleCopyRepository articleCopies;

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/shopping/items")).andExpect(status().isUnauthorized());
    }

    @Test
    void manualEntries() throws Exception {
        UUID text = created(json(post("/api/shopping/items"), "{\"data\":{\"text\":\" Brot \",\"quantity\":2,\"automatic\":true,\"done\":true}}"));
        mvc.perform(get("/api/shopping/items/" + text).with(USER))
                .andExpect(jsonPath("$.data.text").value("Brot"))
                .andExpect(jsonPath("$.data.automatic").value(false))
                .andExpect(jsonPath("$.data.done").value(false));

        UUID known = UUID.randomUUID();
        article(known, 0, false, "Kaffee", 0);
        UUID forArticle = created(json(post("/api/shopping/items"), "{\"data\":{\"articleId\":\"%s\",\"text\":\" \",\"quantity\":1,\"automatic\":false,\"done\":false}}".formatted(known)));
        UUID unknownArticle = created(json(post("/api/shopping/items"), "{\"data\":{\"articleId\":\"%s\",\"quantity\":1,\"automatic\":false,\"done\":false}}".formatted(UUID.randomUUID())));
        mvc.perform(json(post("/api/shopping/items"), "{\"data\":{\"text\":\" \",\"quantity\":1,\"automatic\":false,\"done\":false}}"))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/shopping/items").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + text + "')].label").value(org.hamcrest.Matchers.contains("Brot")))
                .andExpect(jsonPath("$[?(@.id=='" + forArticle + "')].label").value(org.hamcrest.Matchers.contains("Kaffee")))
                .andExpect(jsonPath("$[?(@.id=='" + forArticle + "')].brand").value(org.hamcrest.Matchers.contains("Marke")))
                .andExpect(jsonPath("$[?(@.id=='" + unknownArticle + "')].label").value(org.hamcrest.Matchers.contains("Unbekannter Artikel")));

        mvc.perform(json(put("/api/shopping/items/" + text), "{\"audit\":%s,\"data\":{\"text\":\"Brot\",\"quantity\":3,\"automatic\":false,\"done\":true}}".formatted(audit(0))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/shopping/items/" + text), "{\"data\":{\"text\":\"Brot\",\"quantity\":3,\"automatic\":false,\"done\":true}}"))
                .andExpect(status().isConflict());
        // done entries are listed last
        String list = mvc.perform(get("/api/shopping/items").with(USER)).andReturn().getResponse().getContentAsString();
        List<Boolean> done = JsonPath.read(list, "$[*].done");
        assertThat(done.indexOf(true)).isGreaterThan(done.lastIndexOf(false));

        mvc.perform(delete("/api/shopping/items/done").with(USER)).andExpect(status().isNoContent());
        mvc.perform(get("/api/shopping/items/" + text).with(USER)).andExpect(jsonPath("$.audit.deleted").value(true));
        mvc.perform(delete("/api/shopping/items/" + forArticle).param("version", "0").with(USER)).andExpect(status().isNoContent());
        mvc.perform(get("/api/shopping/items").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + forArticle + "' || @.id=='" + text + "')]").isEmpty());
        mvc.perform(get("/api/shopping/items/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());

        assertThat(kafka.await("shopping_item", text.toString(), 3).getLast().get("audit").get("deleted").asBoolean()).isTrue();
    }

    @Test
    void automaticEntriesFollowMinimumStock() throws Exception {
        UUID article = UUID.randomUUID();
        UUID batch1 = UUID.randomUUID();
        UUID batch2 = UUID.randomUUID();
        batch(batch1, 0, false, article, 1);        // no article copy yet: nothing
        assertThat(automatic(article)).isEmpty();

        article(article, 0, false, "Spülmittel", 3);
        assertThat(automatic(article)).containsExactly(2);
        article(article, 0, false, "Ignoriert", 9); // stale version
        assertThat(automatic(article)).containsExactly(2);

        batch(batch1, 1, false, article, 1);        // unchanged quantity: nothing to save
        batch(batch2, 0, false, article, 1);
        assertThat(automatic(article)).containsExactly(1);
        batch(batch1, 0, false, article, 99);       // stale version
        assertThat(automatic(article)).containsExactly(1);

        // marked as bought, still missing after the next change -> open again
        List<String> ids = JsonPath.read(mvc.perform(get("/api/shopping/items").with(USER)).andReturn().getResponse()
                .getContentAsString(), "$[?(@.articleId=='" + article + "')].id");
        String id = ids.getFirst();
        mvc.perform(json(put("/api/shopping/items/" + id), "{\"audit\":%s,\"data\":{\"articleId\":\"%s\",\"quantity\":1,\"automatic\":true,\"done\":true}}"
                        .formatted(audit(1), article)))
                .andExpect(status().isNoContent());
        batch(batch2, 1, false, article, 1);
        mvc.perform(get("/api/shopping/items/" + id).with(USER)).andExpect(jsonPath("$.data.done").value(false));

        batch(batch2, 2, false, article, 5);
        assertThat(automatic(article)).isEmpty();
        batch(batch2, 3, true, article, 0);
        assertThat(automatic(article)).containsExactly(2);
        article(article, 1, true, "Spülmittel", 3); // deleted article: no entry
        assertThat(automatic(article)).isEmpty();
        article(article, 2, true, "Spülmittel", 3);
        assertThat(automatic(article)).isEmpty();
    }

    @Test
    void copiesArriveViaKafka() {
        UUID article = UUID.randomUUID();
        String audit = audit(0);
        kafka.send("catalog_article", article.toString(),
                "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"name\":\"Salz\",\"minStock\":2,\"eans\":[]}}".formatted(article, audit));
        UUID batch = UUID.randomUUID();
        kafka.send("stock_batch", batch.toString(),
                "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"articleId\":\"%s\",\"quantity\":1,\"locationId\":\"%s\"}}"
                        .formatted(batch, audit, article, UUID.randomUUID()));
        Awaitility.await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(automatic(article)).containsExactly(1));
        assertThat(articleCopies.findByArticleId(article)).isPresent();
    }
}
