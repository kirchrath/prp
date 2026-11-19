package home.inventory.recipe.copy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchDataDto(UUID articleId, int quantity) {
}
