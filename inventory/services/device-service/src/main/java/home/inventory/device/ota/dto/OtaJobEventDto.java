package home.inventory.device.ota.dto;

import home.inventory.device.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code device_ota_job}. */
public record OtaJobEventDto(UUID id, AuditDto audit, OtaJobDataDto data) {
}
