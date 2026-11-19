package home.inventory.notification.settings.dto;

import home.inventory.notification.common.dto.AuditDto;
import java.util.UUID;

public record SettingsEventDto(UUID id, AuditDto audit, SettingsDataDto data) {
}
