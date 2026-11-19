package home.inventory.shopping;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public final class ShoppingDtos {

    private ShoppingDtos() {
    }

    public record ShoppingListItemDto(Long id, @Nullable Long articleId, String label, @Nullable String brand,
                                      int quantity, boolean automatic, boolean done, Instant createdAt) {
        static ShoppingListItemDto of(ShoppingListItem i) {
            var a = i.getArticle();
            String label = a != null ? a.getName() : i.getText();
            return new ShoppingListItemDto(i.getId(), a == null ? null : a.getId(), label == null ? "" : label,
                    a == null ? null : a.getBrand(), i.getQuantity(), i.isAutomatic(), i.isDone(), i.getCreatedAt());
        }
    }

    /** Either {@code articleId} or {@code text} must be given. */
    public record ShoppingListItemRequest(@Nullable Long articleId, @Nullable @Size(max = 200) String text,
                                          @Min(1) int quantity) {
    }

    public record ShoppingListItemUpdate(@Nullable Boolean done, @Nullable @Min(1) Integer quantity) {
    }
}
