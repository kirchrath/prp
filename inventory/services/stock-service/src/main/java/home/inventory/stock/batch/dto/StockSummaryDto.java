package home.inventory.stock.batch.dto;

import java.util.List;

public record StockSummaryDto(long totalUnits, List<StockBatchListDto> expiringSoon) {
}
