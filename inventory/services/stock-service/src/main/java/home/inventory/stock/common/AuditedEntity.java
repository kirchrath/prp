package home.inventory.stock.common;

import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate root with generated UUID, auditing, optimistic locking and soft delete.
 * Implemented by immutable records; Spring Data JDBC populates generated values via the withers.
 */
public interface AuditedEntity<T extends AuditedEntity<T>> {

    UUID id();

    Long version();

    Instant createdDate();

    Instant lastModifiedDate();

    boolean deleted();

    T withId(UUID id);
}
