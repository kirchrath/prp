package home.inventory.recipe.copy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import home.inventory.recipe.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code catalog_article} (owned by the catalog service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArticleEventDto(UUID id, AuditDto audit, ArticleDataDto data) {
}
