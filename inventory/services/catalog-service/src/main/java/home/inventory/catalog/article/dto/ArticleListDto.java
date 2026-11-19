package home.inventory.catalog.article.dto;

import home.inventory.catalog.article.ArticleStatus;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Everything the article list shows. */
public record ArticleListDto(UUID id, String name, @Nullable String brand, @Nullable String packageSize,
                             @Nullable String imageUrl, @Nullable UUID categoryId, @Nullable String categoryName,
                             int minStock, ArticleStatus status, List<String> eans) {
}
