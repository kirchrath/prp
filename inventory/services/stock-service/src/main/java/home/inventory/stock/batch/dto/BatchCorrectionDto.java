package home.inventory.stock.batch.dto;

import home.inventory.stock.common.dto.AuditDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Counted quantity of a batch (0 = used up). */
public record BatchCorrectionDto(@NotNull AuditDto audit, @Min(0) int quantity) {
}
