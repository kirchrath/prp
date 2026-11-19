package home.inventory.recipe.cooking.dto;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A recipe for the requested servings; {@code cookable} if nothing is missing. */
public record RecipeSuggestionDto(UUID recipeId, String title, @Nullable String imageUrl, @Nullable String categoryName,
                                  @Nullable Integer totalMinutes, boolean cookable, List<MissingIngredientDto> missing) {
}
