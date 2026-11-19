package home.inventory.shopping.item.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Either {@code articleId} or {@code text}; {@code automatic} is maintained by the service. */
public record ShoppingItemDataDto(@Nullable UUID articleId, @Nullable @Size(max = 200) String text,
                                  @Min(1) int quantity, boolean automatic, boolean done) {
}
