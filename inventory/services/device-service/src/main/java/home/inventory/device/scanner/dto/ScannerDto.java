package home.inventory.device.scanner.dto;

import home.inventory.device.common.dto.AuditDto;

/** Single scanner; the id is part of the path. */
public record ScannerDto(AuditDto audit, ScannerDataDto data) {
}
