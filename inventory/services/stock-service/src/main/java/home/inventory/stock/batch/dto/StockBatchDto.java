package home.inventory.stock.batch.dto;

import home.inventory.stock.common.dto.AuditDto;

/** Single batch; the id is part of the path. */
public record StockBatchDto(AuditDto audit, StockBatchDataDto data) {
}
