package home.inventory.recipe.shopping.dto;

import home.inventory.recipe.common.dto.AuditDto;

/** Single shopping request; the id is part of the path. */
public record ShoppingRequestDto(AuditDto audit, ShoppingRequestDataDto data) {
}
