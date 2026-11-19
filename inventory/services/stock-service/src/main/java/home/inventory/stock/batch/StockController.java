package home.inventory.stock.batch;

import home.inventory.stock.batch.dto.ArticleLevelDto;
import home.inventory.stock.batch.dto.BatchBestBeforeDto;
import home.inventory.stock.batch.dto.BatchCorrectionDto;
import home.inventory.stock.batch.dto.LocationLevelDto;
import home.inventory.stock.batch.dto.ManualBookingDto;
import home.inventory.stock.batch.dto.StockBatchDto;
import home.inventory.stock.batch.dto.StockBatchListDto;
import home.inventory.stock.batch.dto.StockSummaryDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
@Tag(name = "Stock")
public class StockController {

    private final StockQueryService queries;
    private final ManualBookingService manual;
    private final StockService stock;

    StockController(StockQueryService queries, ManualBookingService manual, StockService stock) {
        this.queries = queries;
        this.manual = manual;
        this.stock = stock;
    }

    @GetMapping("/batches")
    public List<StockBatchListDto> listBatches(@RequestParam(required = false) @Nullable UUID articleId,
                                               @RequestParam(required = false) @Nullable UUID locationId) {
        return queries.listBatches(articleId, locationId);
    }

    @GetMapping("/batches/{id}")
    public StockBatchDto getBatch(@PathVariable UUID id) {
        return queries.getBatch(id);
    }

    @PutMapping("/batches/{id}/quantity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void correctBatch(@PathVariable UUID id, @Valid @RequestBody BatchCorrectionDto request) {
        manual.correct(id, request.audit().version(), request.quantity());
    }

    @PostMapping("/batches/{id}/best-before")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignBestBefore(@PathVariable UUID id, @Valid @RequestBody BatchBestBeforeDto request) {
        stock.assignBestBefore(id, request.audit().version(), request.quantity(), request.bestBefore());
    }

    @PostMapping("/in")
    public LocationLevelDto bookIn(@Valid @RequestBody ManualBookingDto request) {
        return manual.bookIn(request);
    }

    @PostMapping("/out")
    public LocationLevelDto bookOut(@Valid @RequestBody ManualBookingDto request) {
        return manual.bookOut(request);
    }

    /** Total quantity per article. */
    @GetMapping("/levels")
    public List<ArticleLevelDto> listLevels() {
        return queries.levels();
    }

    @GetMapping("/summary")
    public StockSummaryDto getSummary(@RequestParam(defaultValue = "14") int days) {
        return queries.summary(days);
    }
}
