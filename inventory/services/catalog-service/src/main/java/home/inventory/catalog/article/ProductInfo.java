package home.inventory.catalog.article;

import org.jspecify.annotations.Nullable;

/** Product data found in an external product database. */
public record ProductInfo(String name, @Nullable String brand, @Nullable String packageSize,
                          @Nullable String imageUrl, ArticleSource source) {
}
