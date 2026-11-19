package home.inventory.notification.settings.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CategoryThresholdDto(@NotNull UUID categoryId, @Min(0) @Max(365) int days) {
}
