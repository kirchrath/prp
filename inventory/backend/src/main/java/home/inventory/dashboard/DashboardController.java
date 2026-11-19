package home.inventory.dashboard;

import home.inventory.article.Article;
import home.inventory.article.ArticleRepository;
import home.inventory.article.ArticleStatus;
import home.inventory.scanner.Scanner;
import home.inventory.scanner.ScannerRepository;
import home.inventory.shopping.ShoppingListItemRepository;
import home.inventory.stock.StockBatchRepository;
import home.inventory.stock.StockDtos.StockBatchDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard")
@Transactional(readOnly = true)
public class DashboardController {

    public record DashboardDto(long articleCount, long totalUnits, long inboxCount, long belowMinStockCount,
                               long openShoppingItems, long scannersOnline, long scannersTotal,
                               List<StockBatchDto> expiringSoon) {
    }

    private final ArticleRepository articles;
    private final StockBatchRepository batches;
    private final ShoppingListItemRepository shopping;
    private final ScannerRepository scanners;

    DashboardController(ArticleRepository articles, StockBatchRepository batches, ShoppingListItemRepository shopping,
                        ScannerRepository scanners) {
        this.articles = articles;
        this.batches = batches;
        this.shopping = shopping;
        this.scanners = scanners;
    }

    /** Key figures; {@code expiringSoon} contains batches whose best-before date is within {@code days}. */
    @GetMapping
    public DashboardDto getDashboard(@RequestParam(defaultValue = "7") int days) {
        long belowMin = 0;
        for (Article a : articles.findByMinStockGreaterThan(0)) {
            if (batches.totalQuantity(a) < a.getMinStock()) {
                belowMin++;
            }
        }
        List<Scanner> all = scanners.findAll();
        return new DashboardDto(
                articles.count(),
                batches.totalUnits(),
                articles.countByStatus(ArticleStatus.UNKNOWN),
                belowMin,
                shopping.countByDoneFalse(),
                all.stream().filter(Scanner::isOnline).count(),
                all.size(),
                batches.expiringUntil(LocalDate.now().plusDays(Math.max(days, 0))).stream()
                        .map(StockBatchDto::of).toList());
    }
}
