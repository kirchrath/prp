package home.inventory.recipe.shopping.dto;

import home.inventory.recipe.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code recipe_shopping_request}. */
public record ShoppingRequestEventDto(UUID id, AuditDto audit, ShoppingRequestDataDto data) {
}
