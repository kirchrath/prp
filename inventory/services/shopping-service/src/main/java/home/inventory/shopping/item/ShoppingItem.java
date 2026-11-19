package home.inventory.shopping.item;

import home.inventory.shopping.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Entry of the shopping list: an article or free text. Automatic entries follow the minimum stock. */
@Table("shopping_item")
public record ShoppingItem(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        @Nullable UUID articleId,
        @Nullable String text,
        int quantity,
        boolean automatic,
        boolean done) implements AuditedEntity<ShoppingItem> {

    public static ShoppingItem create(@Nullable UUID articleId, @Nullable String text, int quantity, boolean automatic) {
        return new ShoppingItem(null, null, null, null, false, articleId, text, quantity, automatic, false);
    }

    @Override
    public ShoppingItem withId(UUID id) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }

    public ShoppingItem withVersion(Long version) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }

    public ShoppingItem withCreatedDate(Instant createdDate) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }

    public ShoppingItem withLastModifiedDate(Instant lastModifiedDate) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }

    public ShoppingItem withData(@Nullable UUID articleId, @Nullable String text, int quantity, boolean done) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }

    public ShoppingItem withDeleted(boolean deleted) {
        return new ShoppingItem(id, version, createdDate, lastModifiedDate, deleted, articleId, text, quantity, automatic, done);
    }
}
