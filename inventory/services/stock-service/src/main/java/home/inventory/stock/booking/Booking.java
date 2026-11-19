package home.inventory.stock.booking;

import home.inventory.stock.common.AuditedEntity;
import home.inventory.stock.movement.MovementSource;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** A scanned unit to be booked in or out, and its result. */
@Table("booking")
public record Booking(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String ean,
        UUID locationId,
        Direction direction,
        MovementSource source,
        @Nullable UUID scanId,
        @Nullable UUID scannerId,
        @Nullable String username,
        BookingStatus status,
        @Nullable UUID articleId,
        @Nullable String articleName,
        @Nullable Long quantityAtLocation,
        String message) implements AuditedEntity<Booking> {

    public static Booking create(String ean, UUID locationId, Direction direction, MovementSource source,
                                 @Nullable UUID scanId, @Nullable UUID scannerId, @Nullable String username) {
        return new Booking(null, null, null, null, false, ean, locationId, direction, source, scanId, scannerId,
                username, BookingStatus.REQUESTED, null, null, null, "");
    }

    @Override
    public Booking withId(UUID id) {
        return new Booking(id, version, createdDate, lastModifiedDate, deleted, ean, locationId, direction, source,
                scanId, scannerId, username, status, articleId, articleName, quantityAtLocation, message);
    }

    public Booking withVersion(Long version) {
        return new Booking(id, version, createdDate, lastModifiedDate, deleted, ean, locationId, direction, source,
                scanId, scannerId, username, status, articleId, articleName, quantityAtLocation, message);
    }

    public Booking withCreatedDate(Instant createdDate) {
        return new Booking(id, version, createdDate, lastModifiedDate, deleted, ean, locationId, direction, source,
                scanId, scannerId, username, status, articleId, articleName, quantityAtLocation, message);
    }

    public Booking withLastModifiedDate(Instant lastModifiedDate) {
        return new Booking(id, version, createdDate, lastModifiedDate, deleted, ean, locationId, direction, source,
                scanId, scannerId, username, status, articleId, articleName, quantityAtLocation, message);
    }

    public Booking withResult(BookingStatus status, @Nullable UUID articleId, @Nullable String articleName,
                              @Nullable Long quantityAtLocation, String message) {
        return new Booking(id, version, createdDate, lastModifiedDate, deleted, ean, locationId, direction, source,
                scanId, scannerId, username, status, articleId, articleName, quantityAtLocation, message);
    }
}
