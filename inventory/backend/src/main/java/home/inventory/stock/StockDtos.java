package home.inventory.stock;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.jspecify.annotations.Nullable;

public final class StockDtos {

    private StockDtos() {
    }

    public record StockBatchDto(Long id, Long articleId, String articleName, @Nullable String articleBrand,
                                @Nullable String articleImageUrl, Long locationId, String locationPath,
                                @Nullable LocalDate bestBefore, int quantity) {
        public static StockBatchDto of(StockBatch b) {
            var a = b.getArticle();
            var l = b.getLocation();
            return new StockBatchDto(b.getId(), a.getId(), a.getName(), a.getBrand(), a.getImageUrl(), l.getId(),
                    l.getPath(), b.getBestBefore(), b.getQuantity());
        }
    }

    public record BookingRequest(@NotNull Long articleId, @NotNull Long locationId, @Min(1) int quantity,
                                 @Nullable LocalDate bestBefore) {
    }

    public record StockLevelDto(Long articleId, Long locationId, long quantity) {
    }

    public record ScanRequest(@NotBlank @Size(max = 64) String ean, @NotNull Long locationId,
                              @NotNull ScanDirection direction) {
    }

    public record CorrectionRequest(@Min(0) int quantity) {
    }

    public record BestBeforeRequest(@Min(1) int quantity, @Nullable LocalDate bestBefore) {
    }

    public record MovementDto(Long id, Long articleId, String articleName, @Nullable Long locationId,
                              @Nullable String locationPath, @Nullable LocalDate bestBefore, int delta,
                              MovementType type, MovementSource source, @Nullable String scannerName,
                              @Nullable String username, Instant createdAt) {
        public static MovementDto of(StockMovement m) {
            var l = m.getLocation();
            var s = m.getScanner();
            return new MovementDto(m.getId(), m.getArticle().getId(), m.getArticle().getName(),
                    l == null ? null : l.getId(), l == null ? null : l.getPath(), m.getBestBefore(), m.getDelta(),
                    m.getType(), m.getSource(), s == null ? null : s.getDisplayName(), m.getUsername(),
                    m.getCreatedAt());
        }
    }

    public record MovementPage(List<MovementDto> items, int page, int size, long totalElements, int totalPages) {
    }
}
