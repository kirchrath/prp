package home.inventory.notification.copy.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** The part of a {@code stock_batch} event this service needs. */
public record BatchDataDto(UUID articleId, @Nullable LocalDate bestBefore, int quantity) {
}
