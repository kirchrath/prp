package home.inventory.article;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class ArticleDtos {

    private ArticleDtos() {
    }

    public record ArticleDto(Long id, String name, @Nullable String brand, @Nullable String packageSize,
                             @Nullable String imageUrl, @Nullable Long categoryId, @Nullable String categoryName,
                             int minStock, ArticleStatus status, ArticleSource source, List<String> eans,
                             long totalQuantity, Instant createdAt) {

        public static ArticleDto of(Article a, long totalQuantity) {
            var category = a.getCategory();
            return new ArticleDto(a.getId(), a.getName(), a.getBrand(), a.getPackageSize(), a.getImageUrl(),
                    category == null ? null : category.getId(), category == null ? null : category.getName(),
                    a.getMinStock(), a.getStatus(), a.getSource(), List.copyOf(a.getEans()), totalQuantity,
                    a.getCreatedAt());
        }
    }

    public record ArticleRequest(@NotBlank @Size(max = 200) String name,
                                 @Nullable @Size(max = 200) String brand,
                                 @Nullable @Size(max = 100) String packageSize,
                                 @Nullable @Size(max = 1000) String imageUrl,
                                 @Nullable Long categoryId,
                                 @Min(0) int minStock,
                                 @NotNull List<@Pattern(regexp = "[0-9A-Za-z\\-]{1,64}") String> eans) {
    }

    public record ProductLookupDto(boolean found, @Nullable String name, @Nullable String brand,
                                   @Nullable String packageSize, @Nullable String imageUrl,
                                   @Nullable ArticleSource source, @Nullable Long existingArticleId) {
    }
}
