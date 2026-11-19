package home.inventory.shopping.recipe.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import home.inventory.shopping.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code recipe_shopping_request} (owned by the recipe service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ShoppingRequestEventDto(UUID id, AuditDto audit, ShoppingRequestDataDto data) {
}
