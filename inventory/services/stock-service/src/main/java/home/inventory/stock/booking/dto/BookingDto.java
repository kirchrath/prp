package home.inventory.stock.booking.dto;

import home.inventory.stock.common.dto.AuditDto;

/** Single booking; the id is part of the path. */
public record BookingDto(AuditDto audit, BookingDataDto data) {
}
