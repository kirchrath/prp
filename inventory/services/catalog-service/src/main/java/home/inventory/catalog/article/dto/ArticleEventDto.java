package home.inventory.catalog.article.dto;

import home.inventory.catalog.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code catalog_article}. */
public record ArticleEventDto(UUID id, AuditDto audit, ArticleDataDto data) {
}
