package home.inventory.recipe.shopping;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.relational.core.mapping.Table;

/** One entry for the shopping list (part of the shopping request aggregate). */
@Table("shopping_request_item")
public record ShoppingRequestItem(@Nullable UUID articleId, String text, int quantity) {
}
