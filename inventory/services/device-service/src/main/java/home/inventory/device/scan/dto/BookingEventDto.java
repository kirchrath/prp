package home.inventory.device.scan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import home.inventory.device.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_booking} (owned by the stock service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BookingEventDto(UUID id, AuditDto audit, BookingDataDto data) {
}
