package home.inventory.stock.booking.dto;

import home.inventory.stock.booking.BookingStatus;
import home.inventory.stock.booking.Direction;
import home.inventory.stock.movement.MovementSource;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record BookingDataDto(String ean, UUID locationId, Direction direction, MovementSource source,
                             @Nullable UUID scanId, @Nullable UUID scannerId, @Nullable String username,
                             BookingStatus status, @Nullable UUID articleId, @Nullable String articleName,
                             @Nullable Long quantityAtLocation, String message) {
}
