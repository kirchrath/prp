package home.inventory.stock.batch;

import home.inventory.stock.common.AuditedEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Units of one article at one location with the same best-before date (or none).
 * A batch that is used up is soft deleted.
 */
@Table("stock_batch")
public record StockBatch(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID articleId,
        UUID locationId,
        @Nullable LocalDate bestBefore,
        int quantity) implements AuditedEntity<StockBatch> {

    public static StockBatch create(UUID articleId, UUID locationId, @Nullable LocalDate bestBefore) {
        return new StockBatch(null, null, null, null, false, articleId, locationId, bestBefore, 0);
    }

    @Override
    public StockBatch withId(UUID id) {
        return new StockBatch(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore, quantity);
    }

    public StockBatch withVersion(Long version) {
        return new StockBatch(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore, quantity);
    }

    public StockBatch withCreatedDate(Instant createdDate) {
        return new StockBatch(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore, quantity);
    }

    public StockBatch withLastModifiedDate(Instant lastModifiedDate) {
        return new StockBatch(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore, quantity);
    }

    /** New quantity; 0 marks the batch as used up (deleted). */
    public StockBatch withQuantity(int quantity) {
        return new StockBatch(id, version, createdDate, lastModifiedDate, quantity == 0, articleId, locationId, bestBefore,
                quantity);
    }
}
