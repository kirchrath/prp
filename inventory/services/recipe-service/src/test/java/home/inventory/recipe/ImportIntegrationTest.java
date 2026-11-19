package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import home.inventory.recipe.common.AiException;
import java.net.URI;
import java.util.UUID;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class ImportIntegrationTest extends AbstractIntegrationTest {

    @Test
    void importsFreeTextWithTheAi() throws Exception {
        UUID flour = article("Weizenmehl", "1000", "GRAM");
        ai.answer("""
                {"title":" Pfannkuchen ","description":"Klassiker","servings":0,"prepMinutes":-5,"cookMinutes":15,
                 "category":"nachtisch","tags":[" Süß ",""," süß "],"steps":["Verrühren"," ","Ausbacken"],
                 "ingredients":[
                   {"name":"Mehl","amount":250,"unit":"GRAM","basic":false,"articleId":"%s"},
                   {"name":"Milch","amount":0,"unit":"MILLILITER","basic":false,"articleId":"%s"},
                   {"name":"Salz","basic":true},
                   {"name":" ","amount":1,"unit":"PIECE","basic":false},
                   {"name":"Eier","amount":3,"unit":"PIECE","basic":false,"articleId":"keine-id"}]}"""
                .formatted(flour, UUID.randomUUID()));

        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\"250 g Mehl, 3 Eier {Prise} Salz\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Pfannkuchen"))
                .andExpect(jsonPath("$.servings").value(4))
                .andExpect(jsonPath("$.prepMinutes").doesNotExist())
                .andExpect(jsonPath("$.cookMinutes").value(15))
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.tags").value(Matchers.contains("süß")))
                .andExpect(jsonPath("$.steps").value(Matchers.contains("Verrühren", "Ausbacken")))
                .andExpect(jsonPath("$.ingredients.length()").value(4))
                .andExpect(jsonPath("$.ingredients[0].articleId").value(flour.toString()))
                .andExpect(jsonPath("$.ingredients[1].amount").doesNotExist())
                .andExpect(jsonPath("$.ingredients[1].unit").doesNotExist())
                .andExpect(jsonPath("$.ingredients[1].articleId").doesNotExist())
                .andExpect(jsonPath("$.ingredients[3].articleId").doesNotExist());
        String prompt = ai.prompts().getLast();
        assertThat(prompt).contains("Weizenmehl").contains(flour.toString()).contains("{Prise}").contains("Nachtisch");
    }

    @Test
    void importsSchemaOrgRecipePagesAndLetsTheAiStructureIngredients() throws Exception {
        when(pages.fetch(URI.create("https://rezepte.test/lasagne"))).thenReturn("""
                <html><head><script type="application/ld+json">
                {"@type":"Recipe","name":"Lasagne","recipeYield":"6 Portionen","recipeCategory":"Hauptgang",
                 "image":"https://img/l.jpg","recipeIngredient":["500 g Hackfleisch","Salz"],
                 "recipeInstructions":"Schichten.\\nBacken."}</script></head></html>""");
        ai.answer("{\"ingredients\":[{\"name\":\"Hackfleisch\",\"amount\":500,\"unit\":\"GRAM\",\"basic\":false},"
                + "{\"name\":\"Salz\",\"basic\":true}]}");
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\" https://rezepte.test/lasagne\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\"https://rezepte.test/lasagne\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Lasagne"))
                .andExpect(jsonPath("$.servings").value(6))
                .andExpect(jsonPath("$.imageUrl").value("https://img/l.jpg"))
                .andExpect(jsonPath("$.sourceUrl").value("https://rezepte.test/lasagne"))
                .andExpect(jsonPath("$.categoryId").exists())
                .andExpect(jsonPath("$.steps").value(Matchers.contains("Schichten.", "Backen.")))
                .andExpect(jsonPath("$.ingredients[1].basic").value(true));
        assertThat(ai.prompts().getLast()).contains("500 g Hackfleisch\nSalz");

        // recipe page without ingredients: no AI call needed
        int calls = ai.prompts().size();
        when(pages.fetch(URI.create("https://rezepte.test/tee"))).thenReturn(
                "<script type=\"application/ld+json\">{\"@type\":\"Recipe\",\"name\":\"Tee\",\"recipeCategory\":\"Getränk\"}</script>");
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\"https://rezepte.test/tee\"}"))
                .andExpect(jsonPath("$.ingredients").isEmpty())
                .andExpect(jsonPath("$.categoryId").doesNotExist())
                .andExpect(jsonPath("$.servings").value(4));
        assertThat(ai.prompts()).hasSize(calls);
    }

    @Test
    void pagesWithoutStructuredDataGoToTheAi() throws Exception {
        when(pages.fetch(URI.create("https://blog.test/kuchen"))).thenReturn("""
                <html><head><title>Omas Kuchen</title><meta property="og:image" content="https://img/k.jpg"></head>
                <body>200 g Zucker, 3 Eier. Backen.</body></html>""");
        ai.answer("{\"title\":\"\",\"servings\":200,\"tags\":null,\"steps\":null,\"ingredients\":null,\"category\":null}");
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\"https://blog.test/kuchen\"}"))
                .andExpect(jsonPath("$.title").value("Neues Rezept"))
                .andExpect(jsonPath("$.servings").value(100))
                .andExpect(jsonPath("$.imageUrl").value("https://img/k.jpg"))
                .andExpect(jsonPath("$.ingredients").isEmpty());
        assertThat(ai.prompts().getLast()).contains("Omas Kuchen").contains("200 g Zucker");

        when(pages.fetch(URI.create("https://blog.test/ohne-bild"))).thenReturn("<p>Brot</p>");
        ai.answer("{\"title\":\"Brot\",\"servings\":null}");
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\"https://blog.test/ohne-bild\"}"))
                .andExpect(jsonPath("$.imageUrl").doesNotExist())
                .andExpect(jsonPath("$.servings").value(4));
    }

    @Test
    void ignoresIncompleteAiAnswers() throws Exception {
        ai.answer("""
                {"title":null,"servings":2,"tags":[null,"Herzhaft"],"steps":[null,"Rühren"],
                 "ingredients":[{"name":null,"amount":1,"unit":"PIECE","basic":false},{"name":"Ei","basic":false}]}""");
        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\"irgendwas\"}"))
                .andExpect(jsonPath("$.title").value("Neues Rezept"))
                .andExpect(jsonPath("$.tags").value(Matchers.contains("herzhaft")))
                .andExpect(jsonPath("$.steps").value(Matchers.contains("Rühren")))
                .andExpect(jsonPath("$.ingredients.length()").value(1));
    }

    @Test
    void reportsAiAndPageProblems() throws Exception {
        ai.answer(new IllegalStateException("model not found"));
        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\"Rezept\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("model not found")));
        ai.answer("kein json");
        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\"Rezept\"}")).andExpect(status().isBadGateway());
        ai.answer("null");
        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\"Rezept\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(Matchers.containsString("keine verwertbare")));
        when(pages.fetch(any())).thenThrow(new AiException("Die Seite x konnte nicht geladen werden"));
        mvc.perform(json(post("/api/recipe/imports/url"), "{\"url\":\"https://weg.test\"}")).andExpect(status().isBadGateway());
        mvc.perform(json(post("/api/recipe/imports/text"), "{\"text\":\" \"}")).andExpect(status().isBadRequest());
    }
}
