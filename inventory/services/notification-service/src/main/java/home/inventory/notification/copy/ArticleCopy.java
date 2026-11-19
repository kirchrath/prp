package home.inventory.notification.copy;

import home.inventory.notification.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Local copy of a catalog article (topic {@code catalog_article}): name and category for the thresholds. */
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
        @Nullable UUID categoryId) implements AuditedEntity<ArticleCopy> {

    public static ArticleCopy create(UUID articleId) {
        return new ArticleCopy(null, null, null, null, false, articleId, -1, "", null, null);
    }

    @Override
    public ArticleCopy withId(UUID id) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, categoryId);
    }

    public ArticleCopy withVersion(Long version) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, categoryId);
    }

    public ArticleCopy withCreatedDate(Instant createdDate) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, categoryId);
    }

    public ArticleCopy withLastModifiedDate(Instant lastModifiedDate) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, categoryId);
    }

    public ArticleCopy withSource(long sourceVersion, boolean deleted, String name, @Nullable String brand,
                                  @Nullable UUID categoryId) {
        return new ArticleCopy(id, version, createdDate, lastModifiedDate, deleted, articleId, sourceVersion, name, brand, categoryId);
    }
}
