package home.inventory.recipe.category.dto;

import home.inventory.recipe.common.dto.AuditDto;

/** Single category; the id is part of the path. */
public record CategoryDto(AuditDto audit, CategoryDataDto data) {
}
