package home.inventory.shopping.item.dto;

import home.inventory.shopping.common.dto.AuditDto;

/** Single entry; the id is part of the path. */
public record ShoppingItemDto(AuditDto audit, ShoppingItemDataDto data) {
}
