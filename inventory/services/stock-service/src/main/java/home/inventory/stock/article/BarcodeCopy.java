package home.inventory.stock.article;

import home.inventory.stock.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Barcode → article index built from {@code catalog_article}; used to resolve scans. */
@Table("barcode_copy")
public record BarcodeCopy(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String ean,
        UUID articleId) implements AuditedEntity<BarcodeCopy> {

    public static BarcodeCopy create(String ean, UUID articleId) {
        return new BarcodeCopy(null, null, null, null, false, ean, articleId);
    }

    @Override
    public BarcodeCopy withId(UUID id) {
        return new BarcodeCopy(id, version, createdDate, lastModifiedDate, deleted, ean, articleId);
    }

    public BarcodeCopy withVersion(Long version) {
        return new BarcodeCopy(id, version, createdDate, lastModifiedDate, deleted, ean, articleId);
    }

    public BarcodeCopy withCreatedDate(Instant createdDate) {
        return new BarcodeCopy(id, version, createdDate, lastModifiedDate, deleted, ean, articleId);
    }

    public BarcodeCopy withLastModifiedDate(Instant lastModifiedDate) {
        return new BarcodeCopy(id, version, createdDate, lastModifiedDate, deleted, ean, articleId);
    }

    public BarcodeCopy withDeleted(boolean deleted) {
        return new BarcodeCopy(id, version, createdDate, lastModifiedDate, deleted, ean, articleId);
    }
}
