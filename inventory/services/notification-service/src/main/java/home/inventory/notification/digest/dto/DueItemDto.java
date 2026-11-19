package home.inventory.notification.digest.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Packages of an article with the same best-before date (summed over all locations).
 *
 * @param daysLeft days until the best-before date; negative = expired
 */
public record DueItemDto(UUID articleId, String name, @Nullable String brand, LocalDate bestBefore, int quantity,
                         long daysLeft) {
}
