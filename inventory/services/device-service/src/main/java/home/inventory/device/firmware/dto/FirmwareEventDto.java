package home.inventory.device.firmware.dto;

import home.inventory.device.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code device_firmware} (metadata only). */
public record FirmwareEventDto(UUID id, AuditDto audit, FirmwareDataDto data) {
}
