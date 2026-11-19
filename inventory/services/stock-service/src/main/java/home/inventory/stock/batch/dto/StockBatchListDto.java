package home.inventory.stock.batch.dto;

import java.time.LocalDate;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record StockBatchListDto(UUID id, UUID articleId, String articleName, @Nullable String articleBrand,
                                @Nullable String articleImageUrl, UUID locationId, String locationPath,
                                @Nullable LocalDate bestBefore, int quantity) {
}
