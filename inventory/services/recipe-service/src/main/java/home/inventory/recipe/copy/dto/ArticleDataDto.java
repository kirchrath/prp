package home.inventory.recipe.copy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleDataDto(String name, @Nullable String brand, @Nullable BigDecimal contentAmount,
                             @Nullable String contentUnit) {
}
