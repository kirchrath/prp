package home.inventory.stock.movement;

import home.inventory.stock.common.AuditedEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** One stock change (history). */
@Table("movement")
public record Movement(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID articleId,
        UUID locationId,
        @Nullable LocalDate bestBefore,
        int delta,
        MovementType type,
        MovementSource source,
        @Nullable UUID scannerId,
        @Nullable String username,
        @Nullable UUID bookingId) implements AuditedEntity<Movement> {

    public static Movement create(UUID articleId, UUID locationId, @Nullable LocalDate bestBefore, int delta,
                                  MovementType type, BookingContext context) {
        return new Movement(null, null, null, null, false, articleId, locationId, bestBefore, delta, type,
                context.source(), context.scannerId(), context.username(), context.bookingId());
    }

    @Override
    public Movement withId(UUID id) {
        return new Movement(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore,
                delta, type, source, scannerId, username, bookingId);
    }

    public Movement withVersion(Long version) {
        return new Movement(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore,
                delta, type, source, scannerId, username, bookingId);
    }

    public Movement withCreatedDate(Instant createdDate) {
        return new Movement(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore,
                delta, type, source, scannerId, username, bookingId);
    }

    public Movement withLastModifiedDate(Instant lastModifiedDate) {
        return new Movement(id, version, createdDate, lastModifiedDate, deleted, articleId, locationId, bestBefore,
                delta, type, source, scannerId, username, bookingId);
    }
}
