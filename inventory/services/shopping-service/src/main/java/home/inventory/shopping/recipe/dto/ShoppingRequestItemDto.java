package home.inventory.shopping.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ShoppingRequestItemDto(@Nullable UUID articleId, String text, int quantity) {
}
