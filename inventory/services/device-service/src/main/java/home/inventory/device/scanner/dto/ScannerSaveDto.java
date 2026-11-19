package home.inventory.device.scanner.dto;

import home.inventory.device.common.dto.AuditDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record ScannerSaveDto(@NotNull AuditDto audit, @NotNull @Valid ScannerDataDto data) {
}
