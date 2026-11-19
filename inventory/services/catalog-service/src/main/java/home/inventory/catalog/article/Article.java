package home.inventory.catalog.article;

import home.inventory.catalog.common.AuditedEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.MappedCollection;
import org.springframework.data.relational.core.mapping.Table;

@Table("article")
public record Article(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String name,
        @Nullable String brand,
        @Nullable String packageSize,
        @Nullable BigDecimal contentAmount,
        @Nullable ContentUnit contentUnit,
        @Nullable String imageUrl,
        @Nullable UUID categoryId,
        int minStock,
        ArticleStatus status,
        ArticleSource source,
        @Nullable UUID mergedInto,
        @MappedCollection(idColumn = "article_id") Set<ArticleBarcode> barcodes) implements AuditedEntity<Article> {

    public static Article create(String name, ArticleStatus status, ArticleSource source, Set<ArticleBarcode> barcodes) {
        return new Article(null, null, null, null, false, name, null, null, null, null, null, null, 0, status, source, null,
                barcodes);
    }

    @Override
    public Article withId(UUID id) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    public Article withVersion(Long version) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    public Article withCreatedDate(Instant createdDate) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    public Article withLastModifiedDate(Instant lastModifiedDate) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    /** Replaces all user-editable data; an edited article is no longer in the inbox. */
    public Article withData(String name, @Nullable String brand, @Nullable String packageSize,
                            @Nullable PackageContent content, @Nullable String imageUrl, @Nullable UUID categoryId,
                            int minStock, Set<ArticleBarcode> barcodes) {
        BigDecimal contentAmount = content == null ? null : content.amount();
        ContentUnit contentUnit = content == null ? null : content.unit();
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, ArticleStatus.ACTIVE, source, mergedInto, barcodes);
    }

    public Article withProductInfo(@Nullable String brand, @Nullable String packageSize, @Nullable String imageUrl) {
        PackageContent content = PackageContentParser.parse(packageSize).orElse(null);
        BigDecimal contentAmount = content == null ? null : content.amount();
        ContentUnit contentUnit = content == null ? null : content.unit();
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    public Article withBarcodes(Set<ArticleBarcode> barcodes) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    public Article withDeleted(boolean deleted) {
        return new Article(id, version, createdDate, lastModifiedDate, deleted, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, mergedInto, barcodes);
    }

    /** Marks this article as merged into another one (soft deleted, barcodes moved). */
    public Article mergedInto(UUID target) {
        return new Article(id, version, createdDate, lastModifiedDate, true, name, brand, packageSize, contentAmount, contentUnit, imageUrl,
                categoryId, minStock, status, source, target, Set.of());
    }

    public boolean hasEan(String ean) {
        return barcodes.stream().anyMatch(b -> b.ean().equals(ean));
    }
}
