package home.inventory.shopping.copy;

import home.inventory.shopping.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Local copy of a stock batch (topic {@code stock_batch}); only what the stock level needs. */
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
        int quantity) implements AuditedEntity<BatchCopy> {

    public static BatchCopy create(UUID batchId, UUID articleId) {
        return new BatchCopy(null, null, null, null, false, batchId, -1, articleId, 0);
    }

    @Override
    public BatchCopy withId(UUID id) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, quantity);
    }

    public BatchCopy withVersion(Long version) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, quantity);
    }

    public BatchCopy withCreatedDate(Instant createdDate) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, quantity);
    }

    public BatchCopy withLastModifiedDate(Instant lastModifiedDate) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, quantity);
    }

    public BatchCopy withSource(long sourceVersion, boolean deleted, UUID articleId, int quantity) {
        return new BatchCopy(id, version, createdDate, lastModifiedDate, deleted, batchId, sourceVersion, articleId, quantity);
    }
}
