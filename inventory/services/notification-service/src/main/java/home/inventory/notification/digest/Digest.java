package home.inventory.notification.digest;

import home.inventory.notification.common.AuditedEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

/** The daily digest of one day (at most one per day): what was sent and whether it worked. */
@Table("digest")
public record Digest(
        @Id UUID id,
        @Version Long version,
        @CreatedDate Instant createdDate,
        @LastModifiedDate Instant lastModifiedDate,
        boolean deleted,
        LocalDate day,
        DigestStatus status,
        int attempts,
        int expired,
        int dueToday,
        int dueSoon,
        @Nullable String error) implements AuditedEntity<Digest> {

    public static Digest create(LocalDate day) {
        return new Digest(null, null, null, null, false, day, DigestStatus.FAILED, 0, 0, 0, 0, null);
    }

    @Override
    public Digest withId(UUID id) {
        return new Digest(id, version, createdDate, lastModifiedDate, deleted, day, status, attempts, expired, dueToday, dueSoon, error);
    }

    public Digest withVersion(Long version) {
        return new Digest(id, version, createdDate, lastModifiedDate, deleted, day, status, attempts, expired, dueToday, dueSoon, error);
    }

    public Digest withCreatedDate(Instant createdDate) {
        return new Digest(id, version, createdDate, lastModifiedDate, deleted, day, status, attempts, expired, dueToday, dueSoon, error);
    }

    public Digest withLastModifiedDate(Instant lastModifiedDate) {
        return new Digest(id, version, createdDate, lastModifiedDate, deleted, day, status, attempts, expired, dueToday, dueSoon, error);
    }

    /** Result of one attempt. */
    public Digest withResult(DigestStatus status, int expired, int dueToday, int dueSoon, @Nullable String error) {
        return new Digest(id, version, createdDate, lastModifiedDate, deleted, day, status, attempts + 1, expired,
                dueToday, dueSoon, error);
    }
}
