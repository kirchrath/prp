package home.inventory.device.ota;

import home.inventory.device.common.AuditedEntity;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** Firmware update of one device. The token in the download URL is only valid for this job and until {@code expiresAt}. */
@Table("ota_job")
public record OtaJob(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        UUID scannerId,
        String deviceId,
        UUID firmwareId,
        String targetVersion,
        String token,
        Instant expiresAt,
        OtaState state,
        @Nullable String error) implements AuditedEntity<OtaJob> {

    @Override
    public OtaJob withId(UUID id) {
        return new OtaJob(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, firmwareId, targetVersion, token, expiresAt, state, error);
    }

    public OtaJob withVersion(Long version) {
        return new OtaJob(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, firmwareId, targetVersion, token, expiresAt, state, error);
    }

    public OtaJob withCreatedDate(Instant createdDate) {
        return new OtaJob(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, firmwareId, targetVersion, token, expiresAt, state, error);
    }

    public OtaJob withLastModifiedDate(Instant lastModifiedDate) {
        return new OtaJob(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, firmwareId, targetVersion, token, expiresAt, state, error);
    }

    public static OtaJob create(UUID scannerId, String deviceId, UUID firmwareId, String targetVersion, String token,
                                Instant expiresAt) {
        return new OtaJob(null, null, null, null, false, scannerId, deviceId, firmwareId, targetVersion, token,
                expiresAt, OtaState.REQUESTED, null);
    }

    public OtaJob withState(OtaState state, @Nullable String error) {
        return new OtaJob(id, version, createdDate, lastModifiedDate, deleted, scannerId, deviceId, firmwareId,
                targetVersion, token, expiresAt, state, error);
    }
}
