package home.inventory.shopping.recipe;

import home.inventory.shopping.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Marks a recipe shopping request as handled, so replayed events do not add entries twice. */
@Table("processed_request")
public record ProcessedRequest(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID requestId) implements AuditedEntity<ProcessedRequest> {

    public static ProcessedRequest create(UUID requestId) {
        return new ProcessedRequest(null, null, null, null, false, requestId);
    }

    @Override
    public ProcessedRequest withId(UUID id) {
        return new ProcessedRequest(id, version, createdDate, lastModifiedDate, deleted, requestId);
    }

    public ProcessedRequest withVersion(Long version) {
        return new ProcessedRequest(id, version, createdDate, lastModifiedDate, deleted, requestId);
    }

    public ProcessedRequest withCreatedDate(Instant createdDate) {
        return new ProcessedRequest(id, version, createdDate, lastModifiedDate, deleted, requestId);
    }

    public ProcessedRequest withLastModifiedDate(Instant lastModifiedDate) {
        return new ProcessedRequest(id, version, createdDate, lastModifiedDate, deleted, requestId);
    }
}
