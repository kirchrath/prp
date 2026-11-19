package home.inventory.recipe.recipe.dto;

import home.inventory.recipe.common.dto.AuditDto;

/** Single recipe; the id is part of the path. */
public record RecipeDto(AuditDto audit, RecipeDataDto data) {
}
