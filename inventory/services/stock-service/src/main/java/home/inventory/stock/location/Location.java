package home.inventory.stock.location;

import home.inventory.stock.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Storage location; locations form a tree via {@code parentId}. */
@Table("location")
public record Location(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String name,
        @Nullable String description,
        @Nullable UUID parentId) implements AuditedEntity<Location> {

    public static Location create(String name, @Nullable String description, @Nullable UUID parentId) {
        return new Location(null, null, null, null, false, name, description, parentId);
    }

    @Override
    public Location withId(UUID id) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }

    public Location withVersion(Long version) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }

    public Location withCreatedDate(Instant createdDate) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }

    public Location withLastModifiedDate(Instant lastModifiedDate) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }

    public Location withData(String name, @Nullable String description, @Nullable UUID parentId) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }

    public Location withDeleted(boolean deleted) {
        return new Location(id, version, createdDate, lastModifiedDate, deleted, name, description, parentId);
    }
}
