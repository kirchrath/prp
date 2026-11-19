package home.inventory.stock.location.dto;

import home.inventory.stock.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_location}. */
public record LocationEventDto(UUID id, AuditDto audit, LocationDataDto data) {
}
