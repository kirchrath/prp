package home.inventory.recipe.imports;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import home.inventory.recipe.recipe.IngredientUnit;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Ingredient as structured by the AI. */
public record AiIngredient(
        @JsonPropertyDescription("Name der Zutat ohne Menge, z. B. 'Spaghetti'") String name,
        @JsonPropertyDescription("Menge als Zahl, leer wenn keine Menge angegeben ist") @Nullable BigDecimal amount,
        @JsonPropertyDescription("Einheit der Menge") @Nullable IngredientUnit unit,
        @JsonPropertyDescription("true für Grundzutaten, die man immer hat: Salz, Pfeffer, Wasser, Öl, Zucker") boolean basic,
        @JsonPropertyDescription("id des passenden Artikels aus der Artikelliste oder leer") @Nullable String articleId) {
}
