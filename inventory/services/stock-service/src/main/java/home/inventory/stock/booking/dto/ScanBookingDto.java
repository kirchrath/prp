package home.inventory.stock.booking.dto;

import home.inventory.stock.booking.Direction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Camera scan in the browser: books exactly one unit. */
public record ScanBookingDto(@NotBlank @Size(max = 64) String ean, @NotNull UUID locationId,
                             @NotNull Direction direction) {
}
