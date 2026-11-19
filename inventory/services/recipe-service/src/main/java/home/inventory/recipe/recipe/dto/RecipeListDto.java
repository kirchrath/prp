package home.inventory.recipe.recipe.dto;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record RecipeListDto(UUID id, String title, @Nullable String imageUrl, @Nullable UUID categoryId,
                            @Nullable String categoryName, List<String> tags, int servings,
                            @Nullable Integer totalMinutes) {
}
