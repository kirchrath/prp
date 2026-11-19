package home.inventory.notification.digest.dto;

import home.inventory.notification.digest.DigestStatus;
import java.time.LocalDate;
import org.jspecify.annotations.Nullable;

public record DigestDataDto(LocalDate day, DigestStatus status, int attempts, int expired, int dueToday, int dueSoon,
                            @Nullable String error) {
}
