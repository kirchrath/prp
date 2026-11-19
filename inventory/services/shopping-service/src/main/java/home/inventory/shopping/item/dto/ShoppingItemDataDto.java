package home.inventory.shopping.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Either {@code articleId} or {@code text}; {@code automatic} is maintained by the service, {@code note} says what
 * the entry is for (e.g. "für Lasagne").
 */
public record ShoppingItemDataDto(@Nullable UUID articleId, @Nullable @Size(max = 200) String text,
                                  @Min(1) int quantity, boolean automatic, boolean done,
                                  @Nullable @Size(max = 500) String note) {
}
