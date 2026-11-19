package home.inventory.shopping.copy;

import home.inventory.shopping.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Local copy of a catalog article (topic {@code catalog_article}). */
@Table("article_copy")
public record ArticleCopy(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID articleId,
        long sourceVersion,
        String name,
        @Nullable String brand,
        int minStock) implements AuditedEntity<ArticleCopy> {

    public static ArticleCopy create(UUID articleId) {
        return new ArticleCopy(null, null, null, null, false, articleId, -1, "", null, 0);
    }

    @Override
    public ArticleCopy withId(UUID id) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, minStock);
    }

    public ArticleCopy withVersion(Long version) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, minStock);
    }

    public ArticleCopy withCreatedDate(Instant createdDate) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, minStock);
    }

    public ArticleCopy withLastModifiedDate(Instant lastModifiedDate) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, minStock);
    }

    public ArticleCopy withSource(long sourceVersion, boolean deleted, String name, @Nullable String brand, int minStock) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, minStock);
    }
}
