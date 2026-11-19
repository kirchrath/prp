package home.inventory.notification.digest.dto;

import home.inventory.notification.digest.DigestStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Entry of the send log. */
public record DigestListDto(UUID id, LocalDate day, DigestStatus status, int attempts, int expired, int dueToday,
                            int dueSoon, @Nullable String error, Instant lastModifiedDate) {
}
