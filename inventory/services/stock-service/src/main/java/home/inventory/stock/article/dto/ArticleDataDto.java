package home.inventory.stock.article.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The part of a catalog article the stock service keeps. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleDataDto(String name, @Nullable String brand, @Nullable String imageUrl, String status,
                             List<String> eans, @Nullable UUID mergedInto) {
}
