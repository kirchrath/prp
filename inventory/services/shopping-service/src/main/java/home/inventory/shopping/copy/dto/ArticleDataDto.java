package home.inventory.shopping.copy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleDataDto(String name, @Nullable String brand, int minStock) {
}
