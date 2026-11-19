package home.inventory.stock.batch.dto;

import java.util.UUID;

/** Quantity of an article at one location after a booking. */
public record LocationLevelDto(UUID articleId, UUID locationId, long quantity) {
}
