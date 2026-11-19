package home.inventory.recipe.recipe.dto;

import home.inventory.recipe.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code recipe_recipe}. */
public record RecipeEventDto(UUID id, AuditDto audit, RecipeDataDto data) {
}
