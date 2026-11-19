package home.inventory.stock.booking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import home.inventory.stock.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code device_scan} (owned by the device service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ScanEventDto(UUID id, AuditDto audit, ScanDataDto data) {
}
