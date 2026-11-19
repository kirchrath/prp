package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.recipe.category.CategoryService;
import java.util.List;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class RecipeIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    CategoryService categories;

    private UUID category(String name) throws Exception {
        List<String> ids = read("/api/recipe/categories", "$[?(@.name=='" + name + "')].id");
        return UUID.fromString(ids.getFirst());
    }

    private String recipeJson(String title, UUID categoryId, String ingredients) {
        return """
                {"data":{"title":" %s ","description":" ","servings":2,"prepMinutes":10,"cookMinutes":null,
                 "imageUrl":" ","sourceUrl":"https://rezepte.test/x","categoryId":%s,
                 "tags":[" Vegetarisch ","schnell"],"steps":[" Wasser kochen ","Servieren"],
                 "ingredients":[%s]}}""".formatted(title, categoryId == null ? null : "\"" + categoryId + "\"", ingredients);
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mvc.perform(get("/api/recipe/recipes")).andExpect(status().isUnauthorized());
    }

    @Test
    void defaultCategoriesAreCreatedOnce() throws Exception {
        mvc.perform(get("/api/recipe/categories").with(USER))
                .andExpect(jsonPath("$[*].name").value(Matchers.hasItems("Vorspeise", "Hauptgang", "Nachtisch", "Longdrink", "Aperitif")));
        categories.seedDefaults();
        List<String> names = read("/api/recipe/categories", "$[?(@.name=='Hauptgang')].name");
        assertThat(names).hasSize(1);
        assertThat(categories.findByName(" hauptgang ")).isPresent();
    }

    @Test
    void createFilterUpdateAndDelete() throws Exception {
        UUID main = category("Hauptgang");
        String title = unique("Pasta");
        UUID id = created(json(post("/api/recipe/recipes"), recipeJson(title, main,
                "{\"name\":\" Spaghetti \",\"amount\":200.0,\"unit\":\"GRAM\",\"basic\":false},{\"name\":\"Salz\",\"basic\":true}")));

        mvc.perform(get("/api/recipe/recipes/" + id).with(USER))
                .andExpect(jsonPath("$.data.title").value(title))
                .andExpect(jsonPath("$.data.description").doesNotExist())
                .andExpect(jsonPath("$.data.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.data.tags").value(Matchers.contains("schnell", "vegetarisch")))
                .andExpect(jsonPath("$.data.steps").value(Matchers.contains("Wasser kochen", "Servieren")))
                .andExpect(jsonPath("$.data.ingredients[0].name").value("Spaghetti"))
                .andExpect(jsonPath("$.data.ingredients[0].amount").value(200))
                .andExpect(jsonPath("$.data.ingredients[1].basic").value(true))
                .andExpect(jsonPath("$.audit.version").value(0));

        mvc.perform(get("/api/recipe/recipes").param("search", title.toUpperCase()).with(USER))
                .andExpect(jsonPath("$[0].categoryName").value("Hauptgang"))
                .andExpect(jsonPath("$[0].totalMinutes").value(10));
        mvc.perform(get("/api/recipe/recipes").param("search", "spaghetti").param("tag", "VEGETARISCH").param("categoryId", main.toString()).with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.hasItem(id.toString())));
        mvc.perform(get("/api/recipe/recipes").param("tag", "fleisch").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')]").isEmpty());
        mvc.perform(get("/api/recipe/recipes").param("categoryId", UUID.randomUUID().toString()).with(USER))
                .andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/recipe/recipes/tags").with(USER)).andExpect(jsonPath("$").value(Matchers.hasItems("schnell", "vegetarisch")));

        String update = recipeJson(title + " neu", null, "").replace("{\"data\"", "{\"audit\":" + audit(0) + ",\"data\"");
        mvc.perform(json(put("/api/recipe/recipes/" + id), update)).andExpect(status().isNoContent());
        mvc.perform(json(put("/api/recipe/recipes/" + id), update)).andExpect(status().isConflict());
        mvc.perform(json(put("/api/recipe/recipes/" + id), recipeJson("x", null, ""))).andExpect(status().isConflict());
        mvc.perform(get("/api/recipe/recipes").param("search", "neu").with(USER))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].title").value(Matchers.contains(title + " neu")))
                .andExpect(jsonPath("$[?(@.id=='" + id + "')].categoryName").value(Matchers.empty()));

        mvc.perform(delete("/api/recipe/recipes/" + id).param("version", "1").with(USER)).andExpect(status().isNoContent());
        mvc.perform(get("/api/recipe/recipes").with(USER)).andExpect(jsonPath("$[?(@.id=='" + id + "')]").isEmpty());
        mvc.perform(get("/api/recipe/recipes/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());

        JsonNode event = kafka.await("recipe_recipe", id.toString(), 3).getLast();
        assertThat(event.get("audit").get("deleted").asBoolean()).isTrue();
        assertThat(event.get("data").get("ingredients").size()).isZero();
    }

    @Test
    void categoriesCanBeRenamed() throws Exception {
        UUID id = created(json(post("/api/recipe/categories"), "{\"data\":{\"name\":\"%s\"}}".formatted(unique("Brunch"))));
        mvc.perform(json(put("/api/recipe/categories/" + id), "{\"audit\":%s,\"data\":{\"name\":\"Frühstück\"}}".formatted(audit(0))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/recipe/categories/" + id), "{\"audit\":%s,\"data\":{\"name\":\"Frühstück\"}}".formatted(audit(1))))
                .andExpect(status().isNoContent());
        mvc.perform(json(put("/api/recipe/categories/" + id), "{\"data\":{\"name\":\"x\"}}")).andExpect(status().isConflict());
        mvc.perform(get("/api/recipe/categories/" + id).with(USER)).andExpect(jsonPath("$.data.name").value("Frühstück"));
        mvc.perform(get("/api/recipe/categories/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());
    }

    @Test
    void searchesDescriptionsAndShowsRecipesWithoutTimes() throws Exception {
        String word = unique("geheimtipp");
        UUID id = created(json(post("/api/recipe/recipes"), """
                {"data":{"title":"Toast","description":"Ein %s für Eilige","servings":1,"tags":[],"steps":[],"ingredients":[]}}"""
                .formatted(word)));
        mvc.perform(get("/api/recipe/recipes").param("search", word).param("tag", " ").with(USER))
                .andExpect(jsonPath("$[*].id").value(Matchers.contains(id.toString())))
                .andExpect(jsonPath("$[0].totalMinutes").doesNotExist());
        mvc.perform(get("/api/recipe/recipes").param("search", "nirgends-" + word).with(USER)).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void validatesRecipes() throws Exception {
        mvc.perform(json(post("/api/recipe/recipes"), recipeJson("X", UUID.randomUUID(), ""))).andExpect(status().isNotFound());
        mvc.perform(json(post("/api/recipe/recipes"), recipeJson("", null, ""))).andExpect(status().isBadRequest());
        UUID deleted = category("Suppe");
        long version = ((Number) read("/api/recipe/categories/" + deleted, "$.audit.version")).longValue();
        mvc.perform(delete("/api/recipe/categories/" + deleted).param("version", String.valueOf(version)).with(USER))
                .andExpect(status().isNoContent());
        mvc.perform(json(post("/api/recipe/recipes"), recipeJson("X", deleted, ""))).andExpect(status().isNotFound());
        mvc.perform(json(post("/api/recipe/categories"), "{\"data\":{\"name\":\"Suppe\"}}")).andExpect(status().isCreated());
        mvc.perform(json(put("/api/recipe/categories/" + deleted), "{\"audit\":%s,\"data\":{\"name\":\"Hauptgang\"}}".formatted(audit(version + 1))))
                .andExpect(status().isConflict());
    }
}
