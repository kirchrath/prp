package home.inventory.recipe.shopping;

import home.inventory.recipe.common.AuditedEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

/** Missing ingredients of a recipe that should go onto the shopping list (handled by the shopping service). */
@Table("shopping_request")
public record ShoppingRequest(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID recipeId,
        String recipeTitle,
        int servings,
        @MappedCollection(idColumn = "request_id", keyColumn = "position") List<ShoppingRequestItem> items)
        implements AuditedEntity<ShoppingRequest> {

    public static ShoppingRequest create(UUID recipeId, String recipeTitle, int servings, List<ShoppingRequestItem> items) {
        return new ShoppingRequest(null, null, null, null, false, recipeId, recipeTitle, servings, items);
    }

    @Override
    public ShoppingRequest withId(UUID id) {
        return new ShoppingRequest(id, version, createdDate, lastModifiedDate, deleted, recipeId, recipeTitle, servings, items);
    }

    public ShoppingRequest withVersion(Long version) {
        return new ShoppingRequest(id, version, createdDate, lastModifiedDate, deleted, recipeId, recipeTitle, servings, items);
    }

    public ShoppingRequest withCreatedDate(Instant createdDate) {
        return new ShoppingRequest(id, version, createdDate, lastModifiedDate, deleted, recipeId, recipeTitle, servings, items);
    }

    public ShoppingRequest withLastModifiedDate(Instant lastModifiedDate) {
        return new ShoppingRequest(id, version, createdDate, lastModifiedDate, deleted, recipeId, recipeTitle, servings, items);
    }
}
