package home.inventory.notification.copy.dto;

import home.inventory.notification.common.dto.AuditDto;
import java.util.UUID;

public record BatchEventDto(UUID id, AuditDto audit, BatchDataDto data) {
}
