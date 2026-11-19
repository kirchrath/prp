package home.inventory.stock.booking.dto;

import home.inventory.stock.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_booking}. */
public record BookingEventDto(UUID id, AuditDto audit, BookingDataDto data) {
}
