package home.inventory.catalog.article.dto;

import home.inventory.catalog.common.dto.AuditDto;

/** Single article; the id is part of the path. */
public record ArticleDto(AuditDto audit, ArticleDataDto data) {
}
