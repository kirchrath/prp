package home.inventory.stock.movement.dto;

import java.util.List;

public record MovementPageDto(List<MovementListDto> items, int page, int size, long totalElements, int totalPages) {
}
