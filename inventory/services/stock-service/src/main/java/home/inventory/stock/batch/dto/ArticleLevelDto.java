package home.inventory.stock.batch.dto;

import java.util.UUID;

/** Total quantity of an article over all locations. */
public record ArticleLevelDto(UUID articleId, long quantity) {
}
