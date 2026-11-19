package home.inventory.recipe.shopping.dto;

import java.util.List;
import java.util.UUID;

public record ShoppingRequestDataDto(UUID recipeId, String recipeTitle, int servings, List<ShoppingRequestItemDto> items) {
}
