package home.inventory.catalog.article.dto;

import home.inventory.catalog.article.ArticleSource;
import home.inventory.catalog.article.ArticleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Article data. {@code status}, {@code source} and {@code mergedInto} are maintained by the service
 * and ignored when saving.
 */
public record ArticleDataDto(
        @NotBlank @Size(max = 200) String name,
        @Nullable @Size(max = 200) String brand,
        @Nullable @Size(max = 100) String packageSize,
        @Nullable @Size(max = 1000) String imageUrl,
        @Nullable UUID categoryId,
        @Min(0) int minStock,
        @NotNull List<@Pattern(regexp = "[0-9A-Za-z\\-]{1,64}") String> eans,
        @Nullable ArticleStatus status,
        @Nullable ArticleSource source,
        @Nullable UUID mergedInto) {
}
