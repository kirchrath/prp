package home.inventory.article;

import org.jspecify.annotations.Nullable;

/** Product data found in an external product database. */
public record ProductInfo(String ean, String name, @Nullable String brand, @Nullable String packageSize,
                          @Nullable String imageUrl, ArticleSource source) {
}
