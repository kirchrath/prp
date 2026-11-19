package home.inventory.recipe.copy.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import home.inventory.recipe.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_batch} (owned by the stock service). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BatchEventDto(UUID id, AuditDto audit, BatchDataDto data) {
}
