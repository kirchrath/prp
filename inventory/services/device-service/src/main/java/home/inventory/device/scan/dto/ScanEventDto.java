package home.inventory.device.scan.dto;

import home.inventory.device.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code device_scan}. */
public record ScanEventDto(UUID id, AuditDto audit, ScanDataDto data) {
}
