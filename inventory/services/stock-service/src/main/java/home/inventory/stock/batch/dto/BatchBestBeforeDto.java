package home.inventory.stock.batch.dto;

import home.inventory.stock.common.dto.AuditDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

/** Moves {@code quantity} units of a batch to the batch with this best-before date. */
public record BatchBestBeforeDto(@NotNull AuditDto audit, @Min(1) int quantity, @Nullable LocalDate bestBefore) {
}
