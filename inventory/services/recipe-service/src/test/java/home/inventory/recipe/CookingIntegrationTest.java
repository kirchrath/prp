package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.recipe.copy.ArticleCopyRepository;
import home.inventory.recipe.copy.dto.ArticleDataDto;
import home.inventory.recipe.copy.dto.ArticleEventDto;
import home.inventory.recipe.copy.dto.BatchDataDto;
import home.inventory.recipe.copy.dto.BatchEventDto;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

class CookingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    ArticleCopyRepository articleCopies;

    private UUID recipe(String title, String tag, String ingredients) throws Exception {
        return created(json(post("/api/recipe/recipes"), """
                {"data":{"title":"%s","servings":2,"prepMinutes":null,"cookMinutes":20,"tags":["%s"],"steps":[],
                 "ingredients":[%s]}}""".formatted(title, tag, ingredients)));
    }

    @Test
    void suggestsRecipesForServingsAndRequestsMissingIngredients() throws Exception {
        String tag = unique("t");
        UUID pasta = article("Spaghetti", "500", "GRAM");
        UUID tomatoes = article("Tomaten", "400", "GRAM");
        UUID gone = article("Basilikum", null, null);
        stock(pasta, 1);
        stock(tomatoes, 1);
        UUID cookable = recipe("A Pasta", tag, """
                {"name":"Spaghetti","amount":250,"unit":"GRAM","articleId":"%s","basic":false},
                {"name":"Salz","basic":true}""".formatted(pasta));
        UUID almost = recipe("B Pasta mit Sauce", tag, """
                {"name":"Spaghetti","amount":250,"unit":"GRAM","articleId":"%s","basic":false},
                {"name":"Tomaten","amount":500,"unit":"GRAM","articleId":"%s","basic":false},
                {"name":"Basilikum","articleId":"%s","basic":false},
                {"name":"Zucchini","amount":1.5,"unit":"PIECE","basic":false},
                {"name":"Parmesan","basic":false}""".formatted(pasta, tomatoes, gone));

        // 2 servings: B needs 500 g tomatoes (400 g in stock), basil (none), zucchini and parmesan (no article)
        mvc.perform(get("/api/recipe/suggestions").param("servings", "2").param("tag", tag).with(USER))
                .andExpect(jsonPath("$[0].recipeId").value(cookable.toString()))
                .andExpect(jsonPath("$[0].cookable").value(true))
                .andExpect(jsonPath("$[0].totalMinutes").value(20))
                .andExpect(jsonPath("$[1].cookable").value(false))
                .andExpect(jsonPath("$[1].missing.length()").value(4))
                .andExpect(jsonPath("$[1].missing[0].name").value("Tomaten"))
                .andExpect(jsonPath("$[1].missing[0].articleName").value("Tomaten"))
                .andExpect(jsonPath("$[1].missing[0].packages").value(1));
        // 4 servings: 500 g spaghetti needed, 1 package in stock -> recipe A still cookable
        mvc.perform(get("/api/recipe/suggestions").param("servings", "4").param("tag", tag).with(USER))
                .andExpect(jsonPath("$[0].cookable").value(true))
                .andExpect(jsonPath("$[1].missing[0].amount").value(1000))
                .andExpect(jsonPath("$[1].missing.length()").value(4))
                .andExpect(jsonPath("$[1].missing[2].amount").value(3));
        mvc.perform(get("/api/recipe/suggestions").param("servings", "0").with(USER)).andExpect(status().isBadRequest());

        UUID request = created(post("/api/recipe/recipes/" + almost + "/shopping-requests").param("servings", "4").with(USER));
        mvc.perform(get("/api/recipe/shopping-requests/" + request).with(USER))
                .andExpect(jsonPath("$.data.recipeTitle").value("B Pasta mit Sauce"))
                .andExpect(jsonPath("$.data.servings").value(4))
                .andExpect(jsonPath("$.data.items[*].text").value(Matchers.contains(
                        "Tomaten", "Basilikum", "Zucchini (3 Stück)", "Parmesan")))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[1].quantity").value(1))
                .andExpect(jsonPath("$.data.items[2].articleId").doesNotExist());
        JsonNode event = kafka.await("recipe_shopping_request", request.toString(), 1).getFirst();
        assertThat(event.get("data").get("items").size()).isEqualTo(4);

        mvc.perform(post("/api/recipe/recipes/" + cookable + "/shopping-requests").param("servings", "2").with(USER))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/recipe/shopping-requests/" + UUID.randomUUID()).with(USER)).andExpect(status().isNotFound());
    }

    @Test
    void suggestionsShowCategoriesAndUnknownArticles() throws Exception {
        String tag = unique("t");
        List<String> ids = read("/api/recipe/categories", "$[?(@.name=='Nachtisch')].id");
        UUID unknownArticle = UUID.randomUUID();
        UUID recipe = created(json(post("/api/recipe/recipes"), """
                {"data":{"title":"Pudding","servings":2,"categoryId":"%s","tags":["%s"],"steps":[],
                 "ingredients":[{"name":"Puddingpulver","amount":1,"unit":"PACKAGE","articleId":"%s","basic":false}]}}"""
                .formatted(ids.getFirst(), tag, unknownArticle)));
        mvc.perform(get("/api/recipe/suggestions").param("servings", "2").param("tag", tag)
                        .param("categoryId", ids.getFirst()).with(USER))
                .andExpect(jsonPath("$[0].categoryName").value("Nachtisch"))
                .andExpect(jsonPath("$[0].missing[0].articleName").doesNotExist());
        UUID request = created(post("/api/recipe/recipes/" + recipe + "/shopping-requests").param("servings", "2").with(USER));
        mvc.perform(get("/api/recipe/shopping-requests/" + request).with(USER))
                .andExpect(jsonPath("$.data.items[0].text").value("Puddingpulver (1 Packung)"))
                .andExpect(jsonPath("$.data.items[0].articleId").doesNotExist());
    }

    @Test
    void copiesFollowEventsAndIgnoreOldVersions() {
        UUID id = UUID.randomUUID();
        copies.applyArticle(new ArticleEventDto(id, audit(2, false), new ArticleDataDto("Neu", "Marke", null, null)));
        copies.applyArticle(new ArticleEventDto(id, audit(1, false), new ArticleDataDto("Alt", null, null, null)));
        assertThat(articleCopies.findByArticleId(id)).get().extracting("name").isEqualTo("Neu");
        UUID batch = UUID.randomUUID();
        copies.applyBatch(new BatchEventDto(batch, audit(1, false), new BatchDataDto(id, 3)));
        copies.applyBatch(new BatchEventDto(batch, audit(0, false), new BatchDataDto(id, 9)));
        assertThat(copies.packages().get(id)).isEqualTo(3L);
        copies.applyBatch(new BatchEventDto(batch, audit(2, true), new BatchDataDto(id, 0)));
        assertThat(copies.packages()).doesNotContainKey(id);
    }

    @Test
    void copiesArriveViaKafka() {
        UUID article = UUID.randomUUID();
        kafka.send("catalog_article", article.toString(), """
                {"id":"%s","audit":%s,"data":{"name":"Mehl","contentAmount":1000,"contentUnit":"GRAM","eans":[]}}"""
                .formatted(article, audit(0)));
        UUID batch = UUID.randomUUID();
        kafka.send("stock_batch", batch.toString(), "{\"id\":\"%s\",\"audit\":%s,\"data\":{\"articleId\":\"%s\",\"quantity\":2}}"
                .formatted(batch, audit(0), article));
        Awaitility.await().atMost(Duration.ofSeconds(20)).until(() -> Long.valueOf(2).equals(copies.packages().get(article)));
        assertThat(articleCopies.findByArticleId(article)).get().extracting("contentAmount")
                .isEqualTo(new BigDecimal("1000.000"));
        assertThat(List.of(article)).isNotEmpty();
    }
}
