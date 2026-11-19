package home.inventory.device.scanner;

import home.inventory.device.common.AuditedEntity;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** An ESP32 scanner; registers itself via MQTT and is assigned to a location in the UI. */
@Table("scanner")
public record Scanner(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        String deviceId,
        @Nullable String name,
        @Nullable UUID locationId,
        boolean online,
        @Nullable String firmwareVersion,
        @Nullable String ipAddress,
        @Nullable Instant lastSeen,
        @Nullable String lastBootId,
        @Nullable Long lastSeq) implements AuditedEntity<Scanner> {

    public static Scanner create(String deviceId) {
        return new Scanner(null, null, null, null, false, deviceId, null, null, false, null, null, null, null, null);
    }

    @Override
    public Scanner withId(UUID id) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    public Scanner withVersion(Long version) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    public Scanner withCreatedDate(Instant createdDate) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    public Scanner withLastModifiedDate(Instant lastModifiedDate) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    public Scanner withAssignment(@Nullable String name, @Nullable UUID locationId) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    public Scanner withDeleted(boolean deleted) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, online,
                firmwareVersion, ipAddress, lastSeen, lastBootId, lastSeq);
    }

    /** Status message received; a removed scanner that reconnects is registered again (without location). */
    public Scanner seen(boolean online, @Nullable String firmwareVersion, @Nullable String ipAddress, Instant at) {
        UUID location = deleted ? null : locationId;
        return new Scanner(id, version, createdDate, lastModifiedDate, false, deviceId, name, location, online,
                online ? firmwareVersion : this.firmwareVersion, online ? ipAddress : this.ipAddress, at, lastBootId,
                lastSeq);
    }

    /** Remembers the last processed scan message. */
    public Scanner withLastScan(String bootId, long seq, Instant at) {
        return new Scanner(id, version, createdDate, lastModifiedDate, deleted, deviceId, name, locationId, true,
                firmwareVersion, ipAddress, at, bootId, seq);
    }

    /** {@code true} if this (bootId, seq) was processed already (MQTT QoS 1 redelivery). */
    public boolean isDuplicate(String bootId, long seq) {
        return Objects.equals(bootId, lastBootId) && lastSeq != null && seq <= lastSeq;
    }

    public String displayName() {
        return name != null ? name : deviceId;
    }
}
