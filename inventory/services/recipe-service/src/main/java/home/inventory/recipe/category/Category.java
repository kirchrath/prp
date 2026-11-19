package home.inventory.recipe.category;

import home.inventory.recipe.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table("recipe_category")
public record Category(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String name) implements AuditedEntity<Category> {

    public static Category create(String name) {
        return new Category(null, null, null, null, false, name);
    }

    @Override
    public Category withId(UUID id) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }

    public Category withVersion(Long version) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }

    public Category withCreatedDate(Instant createdDate) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }

    public Category withLastModifiedDate(Instant lastModifiedDate) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }

    public Category withName(String name) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }

    public Category withDeleted(boolean deleted) {
        return new Category(id, version, createdDate, lastModifiedDate, deleted, name);
    }
}
