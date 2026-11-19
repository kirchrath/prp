package home.inventory.device.firmware;

import home.inventory.device.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import home.inventory.device.scanner.DeviceKind;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Uploaded firmware image of a device kind (metadata; the image itself is {@link FirmwareContent}). */
@Table("firmware")
public record Firmware(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        DeviceKind kind,
        String firmwareVersion,
        long size,
        String sha256,
        String fileName) implements AuditedEntity<Firmware> {

    @Override
    public Firmware withId(UUID id) {
        return new Firmware(id, version, createdDate, lastModifiedDate, deleted, kind, firmwareVersion, size, sha256, fileName);
    }

    public Firmware withVersion(Long version) {
        return new Firmware(id, version, createdDate, lastModifiedDate, deleted, kind, firmwareVersion, size, sha256, fileName);
    }

    public Firmware withCreatedDate(Instant createdDate) {
        return new Firmware(id, version, createdDate, lastModifiedDate, deleted, kind, firmwareVersion, size, sha256, fileName);
    }

    public Firmware withLastModifiedDate(Instant lastModifiedDate) {
        return new Firmware(id, version, createdDate, lastModifiedDate, deleted, kind, firmwareVersion, size, sha256, fileName);
    }

    public Firmware withDeleted(boolean deleted) {
        return new Firmware(id, version, createdDate, lastModifiedDate, deleted, kind, firmwareVersion, size, sha256, fileName);
    }

    public static Firmware create(DeviceKind kind, String firmwareVersion, long size, String sha256, String fileName) {
        return new Firmware(null, null, null, null, false, kind, firmwareVersion, size, sha256, fileName);
    }
}
