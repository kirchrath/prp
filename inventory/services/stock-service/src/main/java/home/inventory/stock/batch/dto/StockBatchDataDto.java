package home.inventory.stock.batch.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StockBatchDataDto(UUID articleId, UUID locationId, @Nullable LocalDate bestBefore, int quantity) {
}
