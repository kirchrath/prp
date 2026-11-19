package home.inventory.notification.settings.dto;

import home.inventory.notification.common.dto.AuditDto;

/** The settings (there is only one instance). */
public record SettingsDto(AuditDto audit, SettingsDataDto data) {
}
