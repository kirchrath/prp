package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.article.ArticleService;
import home.inventory.location.LocationService;
import home.inventory.location.StorageLocation;
import home.inventory.stock.StockDtos.BestBeforeRequest;
import home.inventory.stock.StockDtos.BookingRequest;
import home.inventory.stock.StockDtos.CorrectionRequest;
import home.inventory.stock.StockDtos.ScanRequest;
import home.inventory.stock.StockDtos.StockBatchDto;
import home.inventory.stock.StockDtos.StockLevelDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
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
@Transactional(noRollbackFor = InsufficientStockException.class)
public class StockController {

    private final StockService stock;
    private final StockBatchRepository batches;
    private final ArticleService articles;
    private final LocationService locations;
    private final ScanService scans;

    StockController(StockService stock, StockBatchRepository batches, ArticleService articles,
                    LocationService locations, ScanService scans) {
        this.stock = stock;
        this.batches = batches;
        this.articles = articles;
        this.locations = locations;
        this.scans = scans;
    }

    /** Stock batches, optionally filtered by article and/or location (including its sub-locations). */
    @GetMapping
    @Transactional(readOnly = true)
    public List<StockBatchDto> listStock(@RequestParam(required = false) @Nullable Long articleId,
                                         @RequestParam(required = false) @Nullable Long locationId) {
        Set<Long> locationIds = locationId == null ? null : locations.subtreeIds(locationId);
        return batches.search(articleId).stream()
                .filter(b -> locationIds == null || locationIds.contains(b.getLocation().getId()))
                .map(StockBatchDto::of)
                .toList();
    }

    @PostMapping("/in")
    public StockLevelDto bookIn(@Valid @RequestBody BookingRequest request) {
        Article article = articles.get(request.articleId());
        StorageLocation location = locations.get(request.locationId());
        long qty = stock.stockIn(article, location, request.quantity(), request.bestBefore(), Booking.ui());
        return new StockLevelDto(article.getId(), location.getId(), qty);
    }

    @PostMapping("/out")
    public StockLevelDto bookOut(@Valid @RequestBody BookingRequest request) {
        Article article = articles.get(request.articleId());
        StorageLocation location = locations.get(request.locationId());
        long qty = stock.stockOut(article, location, request.quantity(), request.bestBefore(), Booking.ui());
        return new StockLevelDto(article.getId(), location.getId(), qty);
    }

    /** Camera scan from the browser: books exactly one unit like an ESP32 scanner would. */
    @PostMapping("/scan")
    public ScanResult scan(@Valid @RequestBody ScanRequest request) {
        return scans.scan(request.ean().trim(), locations.get(request.locationId()), request.direction(),
                Booking.mobile());
    }

    @PutMapping("/batches/{id}/quantity")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void correctBatch(@PathVariable Long id, @Valid @RequestBody CorrectionRequest request) {
        stock.correct(id, request.quantity(), Booking.ui());
    }

    @PostMapping("/batches/{id}/best-before")
    public StockBatchDto assignBestBefore(@PathVariable Long id, @Valid @RequestBody BestBeforeRequest request) {
        return StockBatchDto.of(stock.assignBestBefore(id, request.quantity(), request.bestBefore()));
    }
}
