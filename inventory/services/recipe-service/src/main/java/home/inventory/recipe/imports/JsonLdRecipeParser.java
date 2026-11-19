package home.inventory.recipe.imports;

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Finds schema.org/Recipe data (JSON-LD) in a recipe page. */
@Component
public class JsonLdRecipeParser {

    private static final Pattern NUMBER = Pattern.compile("\\d+");

    private final ObjectMapper json;

    public JsonLdRecipeParser(ObjectMapper json) {
        this.json = json;
    }

    public Optional<JsonLdRecipe> parse(String html) {
        Document document = Jsoup.parse(html);
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                Optional<JsonLdRecipe> recipe = findRecipe(json.readTree(script.data())).map(this::toRecipe);
                if (recipe.isPresent()) {
                    return recipe;
                }
            } catch (JacksonException e) {
                // broken JSON-LD block: try the next one
            }
        }
        return Optional.empty();
    }

    private Optional<JsonNode> findRecipe(JsonNode node) {
        if (node.isArray()) {
            for (JsonNode element : node) {
                Optional<JsonNode> found = findRecipe(element);
                if (found.isPresent()) {
                    return found;
                }
            }
            return Optional.empty();
        }
        if (!node.isObject()) {
            return Optional.empty();
        }
        if (strings(node.path("@type")).contains("Recipe") && !text(node.path("name")).isEmpty()) {
            return Optional.of(node);
        }
        return findRecipe(node.path("@graph"));
    }

    private JsonLdRecipe toRecipe(JsonNode r) {
        List<String> keywords = new ArrayList<>();
        for (String k : strings(r.path("keywords"))) {
            Arrays.stream(k.split(",")).map(String::trim).filter(s -> !s.isEmpty()).forEach(keywords::add);
        }
        List<String> categories = strings(r.path("recipeCategory"));
        return new JsonLdRecipe(text(r.path("name")), blankToNull(text(r.path("description"))), servings(r.path("recipeYield")),
                minutes(r.path("prepTime")), minutes(r.path("cookTime")), image(r.path("image")),
                categories.isEmpty() ? null : categories.getFirst(), keywords, steps(r.path("recipeInstructions")),
                strings(r.path("recipeIngredient")));
    }

    /** A string, a number or an array of them, as list of non-blank strings. */
    static List<String> strings(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode element : node) {
                result.addAll(strings(element));
            }
        } else if (node.isString() || node.isNumber()) {
            String value = Jsoup.parse(node.asString()).text().trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result;
    }

    private static String text(JsonNode node) {
        List<String> values = strings(node);
        return values.isEmpty() ? "" : values.getFirst();
    }

    /** Steps from a text, a list of texts, HowToStep objects or HowToSection objects. */
    static List<String> steps(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            for (JsonNode element : node) {
                result.addAll(steps(element));
            }
        } else if (node.isObject()) {
            if (node.has("itemListElement")) {
                result.addAll(steps(node.path("itemListElement")));
            } else {
                JsonNode text = node.path("text");
                result.addAll(steps(text.isString() ? text : node.path("name")));
            }
        } else if (node.isString()) {
            // split before removing HTML: line breaks separate the steps
            for (String line : node.asString().split("\\R|<br\\s*/?>")) {
                String text = Jsoup.parse(line).text().trim();
                if (!text.isEmpty()) {
                    result.add(text);
                }
            }
        }
        return result;
    }

    static @Nullable Integer servings(JsonNode node) {
        Matcher m = NUMBER.matcher(text(node));
        return m.find() ? Integer.valueOf(m.group()) : null;
    }

    static @Nullable Integer minutes(JsonNode node) {
        String value = text(node);
        if (value.isEmpty()) {
            return null;
        }
        try {
            return Math.toIntExact(Duration.parse(value).toMinutes());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    static @Nullable String image(JsonNode node) {
        if (node.isArray()) {
            return node.isEmpty() ? null : image(node.get(0));
        }
        return blankToNull(text(node.isObject() ? node.path("url") : node));
    }

    private static @Nullable String blankToNull(String s) {
        return s.isBlank() ? null : s;
    }
}
