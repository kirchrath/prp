package home.inventory.catalog.category.dto;

import home.inventory.catalog.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code catalog_category}. */
public record CategoryEventDto(UUID id, AuditDto audit, CategoryDataDto data) {
}
