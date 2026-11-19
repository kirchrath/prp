package home.inventory.shopping.item.dto;

import home.inventory.shopping.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code shopping_item}. */
public record ShoppingItemEventDto(UUID id, AuditDto audit, ShoppingItemDataDto data) {
}
