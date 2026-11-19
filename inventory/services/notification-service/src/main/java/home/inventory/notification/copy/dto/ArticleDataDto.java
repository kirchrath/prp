package home.inventory.notification.copy.dto;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The part of a {@code catalog_article} event this service needs; other fields are ignored. */
public record ArticleDataDto(String name, @Nullable String brand, @Nullable UUID categoryId) {
}
