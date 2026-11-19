package home.inventory.device.scan;

import home.inventory.device.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** A barcode scanned by an ESP32 scanner and its result. */
@Table("scan")
public record Scan(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID scannerId,
        String deviceId,
        String ean,
        String direction,
        @Nullable UUID locationId,
        long seq,
        String bootId,
        ScanStatus status,
        @Nullable String result,
        @Nullable String articleName,
        @Nullable Long quantity,
        @Nullable String message,
        @Nullable UUID bookingId) implements AuditedEntity<Scan> {

    public static Scan create(UUID scannerId, String deviceId, String ean, String direction, @Nullable UUID locationId,
                              long seq, String bootId) {
        return new Scan(null, null, null, null, false, scannerId, deviceId, ean, direction, locationId, seq, bootId,
                ScanStatus.REQUESTED, null, null, null, null, null);
    }

    @Override
    public Scan withId(UUID id) {
        return new Scan(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, ean, direction,
                locationId, seq, bootId, status, result, articleName, quantity, message, bookingId);
    }

    public Scan withVersion(Long version) {
        return new Scan(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, ean, direction,
                locationId, seq, bootId, status, result, articleName, quantity, message, bookingId);
    }

    public Scan withCreatedDate(Instant createdDate) {
        return new Scan(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, ean, direction,
                locationId, seq, bootId, status, result, articleName, quantity, message, bookingId);
    }

    public Scan withLastModifiedDate(Instant lastModifiedDate) {
        return new Scan(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, ean, direction,
                locationId, seq, bootId, status, result, articleName, quantity, message, bookingId);
    }

    public Scan withResult(ScanStatus status, String result, @Nullable String articleName, @Nullable Long quantity,
                           String message, @Nullable UUID bookingId) {
        return new Scan(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, ean, direction,
                locationId, seq, bootId, status, result, articleName, quantity, message, bookingId);
    }
}
