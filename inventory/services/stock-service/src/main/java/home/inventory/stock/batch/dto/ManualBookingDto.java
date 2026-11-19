package home.inventory.stock.batch.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Manual booking of several units in the web UI. */
public record ManualBookingDto(@NotNull UUID articleId, @NotNull UUID locationId, @Min(1) int quantity,
                               @Nullable LocalDate bestBefore) {
}
