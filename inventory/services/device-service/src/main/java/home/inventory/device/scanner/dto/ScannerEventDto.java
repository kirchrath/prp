package home.inventory.device.scanner.dto;

import home.inventory.device.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code device_scanner}. */
public record ScannerEventDto(UUID id, AuditDto audit, ScannerDataDto data) {
}
