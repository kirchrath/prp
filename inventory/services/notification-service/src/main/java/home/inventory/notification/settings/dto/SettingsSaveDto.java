package home.inventory.notification.settings.dto;

import home.inventory.notification.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** Update: audit of the edited state (optimistic locking) and the new data. */
public record SettingsSaveDto(@NotNull AuditDto audit, @NotNull @Valid SettingsDataDto data) {
}
