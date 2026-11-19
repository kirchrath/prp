package home.inventory.shopping.common.dto;

import java.time.Instant;

/** Technical metadata of every aggregate; sent back unchanged on updates for optimistic locking. */
public record AuditDto(Instant createdDate, Instant lastModifiedDate, Long version, boolean deleted) {
}
