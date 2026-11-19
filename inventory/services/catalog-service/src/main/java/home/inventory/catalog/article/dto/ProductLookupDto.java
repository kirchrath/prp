package home.inventory.catalog.article.dto;

import home.inventory.catalog.article.ArticleSource;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Result of a product database lookup; {@code existingArticleId} if the barcode is already assigned. */
public record ProductLookupDto(boolean found, @Nullable String name, @Nullable String brand,
                               @Nullable String packageSize, @Nullable String imageUrl,
                               @Nullable ArticleSource source, @Nullable UUID existingArticleId) {
}
