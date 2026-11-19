package home.inventory.shopping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import home.inventory.shopping.recipe.RecipeRequestService;
import home.inventory.shopping.recipe.dto.ShoppingRequestDataDto;
import home.inventory.shopping.recipe.dto.ShoppingRequestEventDto;
import home.inventory.shopping.recipe.dto.ShoppingRequestItemDto;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RecipeRequestIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    RecipeRequestService requests;

    private List<Map<String, Object>> items() throws Exception {
        return JsonPath.read(mvc.perform(get("/api/shopping/items").with(USER)).andReturn().getResponse().getContentAsString(), "$");
    }

    private Map<String, Object> item(String label) throws Exception {
        return items().stream().filter(i -> label.equals(i.get("label"))).findFirst().orElseThrow();
    }

    private void request(UUID id, boolean deleted, String recipe, int servings, ShoppingRequestItemDto... items) {
        requests.handle(new ShoppingRequestEventDto(id, audit(0, deleted), new ShoppingRequestDataDto(recipe, servings, List.of(items))));
    }

    @Test
    void addsMissingIngredientsAndMergesDuplicates() throws Exception {
        UUID pasta = UUID.randomUUID();
        article(pasta, 0, false, "Spaghetti-" + pasta, 0);
        String zucchini = "Zucchini-" + UUID.randomUUID() + " (2 Stück)";
        UUID first = UUID.randomUUID();
        request(first, false, "Lasagne", 4, new ShoppingRequestItemDto(pasta, "Spaghetti", 1),
                new ShoppingRequestItemDto(null, zucchini, 1));

        Map<String, Object> spaghetti = item("Spaghetti-" + pasta);
        assertThat(spaghetti.get("quantity")).isEqualTo(1);
        assertThat(spaghetti.get("note")).isEqualTo("für Lasagne (4 Pers.)");
        assertThat(spaghetti.get("automatic")).isEqualTo(false);
        assertThat(item(zucchini).get("quantity")).isEqualTo(1);

        // replayed event: nothing happens; deleted request: ignored
        request(first, false, "Lasagne", 4, new ShoppingRequestItemDto(pasta, "Spaghetti", 1));
        request(UUID.randomUUID(), true, "Lasagne", 4, new ShoppingRequestItemDto(pasta, "Spaghetti", 5));
        assertThat(item("Spaghetti-" + pasta).get("quantity")).isEqualTo(1);

        // same recipe again and another recipe: quantities are added up, the note names each recipe once
        request(UUID.randomUUID(), false, "Lasagne", 4, new ShoppingRequestItemDto(pasta, "Spaghetti", 1),
                new ShoppingRequestItemDto(null, zucchini.toUpperCase(), 2));
        request(UUID.randomUUID(), false, "Nudelsalat", 2, new ShoppingRequestItemDto(pasta, "Spaghetti", 1));
        spaghetti = item("Spaghetti-" + pasta);
        assertThat(spaghetti.get("quantity")).isEqualTo(3);
        assertThat(spaghetti.get("note")).isEqualTo("für Lasagne (4 Pers.), für Nudelsalat (2 Pers.)");
        assertThat(item(zucchini).get("quantity")).isEqualTo(3);

        // a done entry is not reused
        String id = (String) spaghetti.get("id");
        long version = ((Number) JsonPath.read(mvc.perform(get("/api/shopping/items/" + id).with(USER)).andReturn()
                .getResponse().getContentAsString(), "$.audit.version")).longValue();
        mvc.perform(json(put("/api/shopping/items/" + id), """
                {"audit":%s,"data":{"articleId":"%s","quantity":3,"automatic":false,"done":true,"note":" "}}"""
                        .formatted(audit(version), pasta)))
                .andExpect(status().isNoContent());
        request(UUID.randomUUID(), false, "Lasagne", 4, new ShoppingRequestItemDto(pasta, "Spaghetti", 1));
        assertThat(items().stream().filter(i -> pasta.toString().equals(i.get("articleId"))).count()).isEqualTo(2);
        mvc.perform(get("/api/shopping/items/" + id).with(USER)).andExpect(jsonPath("$.data.note").doesNotExist());
    }

    @Test
    void manualEntriesKeepTheirNote() throws Exception {
        UUID id = created(json(post("/api/shopping/items"),
                "{\"data\":{\"text\":\"Kerzen\",\"quantity\":1,\"automatic\":false,\"done\":false,\"note\":\" Geburtstag \"}}"));
        mvc.perform(get("/api/shopping/items/" + id).with(USER)).andExpect(jsonPath("$.data.note").value("Geburtstag"));
    }

    @Test
    void requestsArriveViaKafka() {
        String text = "Basilikum-" + UUID.randomUUID();
        UUID request = UUID.randomUUID();
        kafka.send("recipe_shopping_request", request.toString(), """
                {"id":"%s","audit":%s,"data":{"recipeId":"%s","recipeTitle":"Pesto","servings":2,
                 "items":[{"text":"%s","quantity":1}]}}""".formatted(request, audit(0), UUID.randomUUID(), text));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> items().stream().anyMatch(i -> text.equals(i.get("label"))));
    }
}
