package home.inventory.recipe.shopping.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Entry for the shopping list: an article (quantity = packages) or a free text. */
public record ShoppingRequestItemDto(@Nullable UUID articleId, String text, int quantity) {
}
