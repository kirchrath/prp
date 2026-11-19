package home.inventory.stock.location.dto;

import home.inventory.stock.common.dto.AuditDto;

/** Single location; the id is part of the path. */
public record LocationDto(AuditDto audit, LocationDataDto data) {
}
