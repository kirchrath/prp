package home.inventory.stock.movement.dto;

import home.inventory.stock.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_movement}. */
public record MovementEventDto(UUID id, AuditDto audit, MovementDataDto data) {
}
