package home.inventory.shopping.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShoppingRequestDataDto(String recipeTitle, int servings, List<ShoppingRequestItemDto> items) {
}
