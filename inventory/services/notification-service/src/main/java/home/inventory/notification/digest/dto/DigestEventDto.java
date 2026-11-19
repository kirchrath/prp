package home.inventory.notification.digest.dto;

import home.inventory.notification.common.dto.AuditDto;
import java.util.UUID;

public record DigestEventDto(UUID id, AuditDto audit, DigestDataDto data) {
}
