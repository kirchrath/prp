package home.inventory.stock.batch.dto;

import home.inventory.stock.common.dto.AuditDto;
import java.util.UUID;

/** Payload of topic {@code stock_batch}. */
public record StockBatchEventDto(UUID id, AuditDto audit, StockBatchDataDto data) {
}
