package home.inventory.recipe.imports;

import java.util.List;

/** Answer of the AI when structuring ingredient lines. */
public record AiIngredients(List<AiIngredient> ingredients) {
}
