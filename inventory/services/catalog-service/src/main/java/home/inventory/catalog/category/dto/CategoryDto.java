package home.inventory.catalog.category.dto;

import home.inventory.catalog.common.dto.AuditDto;

/** Single category; the id is part of the path. */
public record CategoryDto(AuditDto audit, CategoryDataDto data) {
}
