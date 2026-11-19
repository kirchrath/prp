package home.inventory.stock.movement.dto;

import home.inventory.stock.movement.MovementSource;
import home.inventory.stock.movement.MovementType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record MovementListDto(UUID id, Instant createdDate, UUID articleId, String articleName, UUID locationId,
                              String locationPath, @Nullable LocalDate bestBefore, int delta, MovementType type,
                              MovementSource source, @Nullable UUID scannerId, @Nullable String username) {
}
