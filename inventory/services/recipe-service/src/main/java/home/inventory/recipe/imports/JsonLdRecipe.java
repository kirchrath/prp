package home.inventory.recipe.imports;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Recipe data as published by a recipe portal (schema.org/Recipe); ingredients still as text lines. */
public record JsonLdRecipe(String title, @Nullable String description, @Nullable Integer servings,
                           @Nullable Integer prepMinutes, @Nullable Integer cookMinutes, @Nullable String imageUrl,
                           @Nullable String category, List<String> tags, List<String> steps,
                           List<String> ingredients) {
}
