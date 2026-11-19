package home.inventory.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import home.inventory.recipe.imports.JsonLdRecipe;
import home.inventory.recipe.imports.JsonLdRecipeParser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JsonLdRecipeParserTest {

    private final JsonLdRecipeParser parser = new JsonLdRecipeParser(JsonMapper.builder().build());

    private Optional<JsonLdRecipe> parse(String... blocks) {
        StringBuilder html = new StringBuilder("<html><head>");
        for (String block : blocks) {
            html.append("<script type=\"application/ld+json\">").append(block).append("</script>");
        }
        return parser.parse(html.append("</head><body>x</body></html>").toString());
    }

    @Test
    void readsATypicalRecipePage() {
        JsonLdRecipe r = parse("""
                {"@context":"https://schema.org","@graph":[{"@type":"WebSite","name":"Portal"},
                 {"@type":["Recipe","NewsArticle"],"name":"Spaghetti &amp; Tomaten","description":"<p>Schnell</p>",
                  "recipeYield":["4","4 Portionen"],"prepTime":"PT15M","cookTime":"PT1H5M",
                  "image":[{"@type":"ImageObject","url":"https://img/1.jpg"}],
                  "recipeCategory":["Hauptgericht","Pasta"],"keywords":"pasta, schnell, ,vegetarisch",
                  "recipeIngredient":["500 g Spaghetti","1 Dose Tomaten",""],
                  "recipeInstructions":[{"@type":"HowToSection","name":"Sauce","itemListElement":[
                     {"@type":"HowToStep","text":"Tomaten kochen."},{"@type":"HowToStep","name":"Würzen."}]},
                     "Nudeln kochen.\\n\\nAbgießen."]}]}""").orElseThrow();
        assertThat(r.title()).isEqualTo("Spaghetti & Tomaten");
        assertThat(r.description()).isEqualTo("Schnell");
        assertThat(r.servings()).isEqualTo(4);
        assertThat(r.prepMinutes()).isEqualTo(15);
        assertThat(r.cookMinutes()).isEqualTo(65);
        assertThat(r.imageUrl()).isEqualTo("https://img/1.jpg");
        assertThat(r.category()).isEqualTo("Hauptgericht");
        assertThat(r.tags()).containsExactly("pasta", "schnell", "vegetarisch");
        assertThat(r.ingredients()).containsExactly("500 g Spaghetti", "1 Dose Tomaten");
        assertThat(r.steps()).containsExactly("Tomaten kochen.", "Würzen.", "Nudeln kochen.", "Abgießen.");
    }

    @Test
    void handlesMinimalAndUnusualData() {
        JsonLdRecipe r = parse("not json", "[{\"@type\":\"Person\"},{\"@type\":\"Recipe\",\"name\":\"Brot\",\"recipeYield\":2,"
                + "\"prepTime\":\"20 Minuten\",\"image\":\"https://img/b.jpg\",\"keywords\":[\"backen\"],"
                + "\"recipeInstructions\":\"Kneten.\\nBacken.\"}]").orElseThrow();
        assertThat(r.servings()).isEqualTo(2);
        assertThat(r.prepMinutes()).isNull();
        assertThat(r.cookMinutes()).isNull();
        assertThat(r.imageUrl()).isEqualTo("https://img/b.jpg");
        assertThat(r.description()).isNull();
        assertThat(r.category()).isNull();
        assertThat(r.tags()).containsExactly("backen");
        assertThat(r.steps()).containsExactly("Kneten.", "Backen.");
        assertThat(r.ingredients()).isEmpty();

        JsonLdRecipe noYield = parse("{\"@type\":\"Recipe\",\"name\":\"Tee\",\"recipeYield\":\"eine Kanne\",\"image\":[],"
                + "\"recipeInstructions\":[{\"@type\":\"HowToStep\"}]}").orElseThrow();
        assertThat(noYield.servings()).isNull();
        assertThat(noYield.imageUrl()).isNull();
        assertThat(noYield.steps()).isEmpty();

        JsonLdRecipe objectImage = parse("{\"@type\":\"Recipe\",\"name\":\"Saft\",\"image\":{\"url\":\" \"}}").orElseThrow();
        assertThat(objectImage.imageUrl()).isNull();
    }

    @Test
    void noRecipe() {
        assertThat(parse()).isEmpty();
        assertThat(parse("{\"@type\":\"Recipe\"}", "\"text\"", "{\"@type\":\"Organization\",\"name\":\"x\"}", "[]")).isEmpty();
        assertThat(parser.parse("<html><body>Keine Daten</body></html>")).isEmpty();
        assertThat(List.of()).isEmpty();
    }
}
