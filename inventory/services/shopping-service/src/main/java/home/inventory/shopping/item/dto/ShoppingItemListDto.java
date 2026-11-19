package home.inventory.shopping.item.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Entry as shown in the list; {@code label} is the article name or the free text. */
public record ShoppingItemListDto(UUID id, @Nullable UUID articleId, String label, @Nullable String brand,
                                  int quantity, boolean automatic, boolean done) {
}
