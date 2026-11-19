package home.inventory.notification.copy;

import home.inventory.notification.common.AuditedEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Local copy of a stock batch (topic {@code stock_batch}): article, best-before date and quantity. */
@Table("batch_copy")
public record BatchCopy(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID batchId,
        long sourceVersion,
        UUID articleId,
        @Nullable LocalDate bestBefore,
        int quantity) implements AuditedEntity<BatchCopy> {

    public static BatchCopy create(UUID batchId, UUID articleId) {
        return new BatchCopy(null, null, null, null, false, batchId, -1, articleId, null, 0);
    }

    @Override
    public BatchCopy withId(UUID id) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, bestBefore, quantity);
    }

    public BatchCopy withVersion(Long version) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, bestBefore, quantity);
    }

    public BatchCopy withCreatedDate(Instant createdDate) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, bestBefore, quantity);
    }

    public BatchCopy withLastModifiedDate(Instant lastModifiedDate) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, bestBefore, quantity);
    }

    public BatchCopy withSource(long sourceVersion, boolean deleted, UUID articleId, @Nullable LocalDate bestBefore,
                                int quantity) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, bestBefore, quantity);
    }
}
