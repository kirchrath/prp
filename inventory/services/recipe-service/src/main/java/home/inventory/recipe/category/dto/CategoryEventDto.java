package home.inventory.recipe.category.dto;

import home.inventory.recipe.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code recipe_category}. */
public record CategoryEventDto(UUID id, AuditDto audit, CategoryDataDto data) {
}
