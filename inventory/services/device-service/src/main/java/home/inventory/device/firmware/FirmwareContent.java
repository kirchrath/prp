package home.inventory.device.firmware;

import home.inventory.device.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** The bytes of an uploaded firmware image (separate aggregate, so listing firmware does not load the images). */
@Table("firmware_content")
public record FirmwareContent(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID firmwareId,
        byte[] content) implements AuditedEntity<FirmwareContent> {

    @Override
    public FirmwareContent withId(UUID id) {
        return new FirmwareContent(id, version, createdDate, lastModifiedDate, deleted, firmwareId, content);
    }

    public FirmwareContent withVersion(Long version) {
        return new FirmwareContent(id, version, createdDate, lastModifiedDate, deleted, firmwareId, content);
    }

    public FirmwareContent withCreatedDate(Instant createdDate) {
        return new FirmwareContent(id, version, createdDate, lastModifiedDate, deleted, firmwareId, content);
    }

    public FirmwareContent withLastModifiedDate(Instant lastModifiedDate) {
        return new FirmwareContent(id, version, createdDate, lastModifiedDate, deleted, firmwareId, content);
    }

    public static FirmwareContent create(UUID firmwareId, byte[] content) {
        return new FirmwareContent(null, null, null, null, false, firmwareId, content);
    }
}
