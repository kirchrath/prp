package home.inventory.stock.movement;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Who/what caused a stock change; stored on every movement. */
public record BookingContext(MovementSource source, @Nullable UUID scannerId, @Nullable String username,
                             @Nullable UUID bookingId) {
}
