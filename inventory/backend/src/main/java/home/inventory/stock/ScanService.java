package home.inventory.stock;

import home.inventory.article.Article;
import home.inventory.article.ArticleService;
import home.inventory.article.ArticleStatus;
import home.inventory.location.StorageLocation;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Books one scanned unit in or out; shared by the ESP32 scanners and the camera scan in the browser. */
@Service
@Transactional
public class ScanService {

    private final ArticleService articles;
    private final StockService stock;

    ScanService(ArticleService articles, StockService stock) {
        this.articles = articles;
        this.stock = stock;
    }

    public ScanResult scan(String ean, StorageLocation location, ScanDirection direction, Booking booking) {
        return switch (direction) {
            case IN -> scanIn(ean, location, booking);
            case OUT -> scanOut(ean, location, booking);
        };
    }

    private ScanResult scanIn(String ean, StorageLocation location, Booking booking) {
        ArticleService.Resolved resolved = articles.resolveOrCreate(ean);
        Article article = resolved.article();
        long quantity = stock.stockIn(article, location, 1, null, booking);
        ScanOutcome outcome = !resolved.created() ? ScanOutcome.IN_OK
                : article.getStatus() == ArticleStatus.UNKNOWN ? ScanOutcome.IN_OK_UNKNOWN_ARTICLE
                : ScanOutcome.IN_OK_NEW_ARTICLE;
        String message = switch (outcome) {
            case IN_OK_UNKNOWN_ARTICLE -> "Unbekannter Artikel eingelagert – bitte in der Inbox ergänzen";
            case IN_OK_NEW_ARTICLE -> article.getName() + " neu angelegt und eingelagert";
            default -> article.getName() + " eingelagert";
        };
        return new ScanResult(outcome, article.getId(), article.getName(), quantity, message);
    }

    private ScanResult scanOut(String ean, StorageLocation location, Booking booking) {
        Optional<Article> found = articles.findByEan(ean);
        if (found.isEmpty()) {
            return ScanResult.failure(ScanOutcome.UNKNOWN_EAN, "Unbekannter Barcode " + ean);
        }
        Article article = found.get();
        try {
            long quantity = stock.stockOut(article, location, 1, null, booking);
            return new ScanResult(ScanOutcome.OUT_OK, article.getId(), article.getName(), quantity,
                    article.getName() + " ausgelagert");
        } catch (InsufficientStockException e) {
            return new ScanResult(ScanOutcome.NOT_IN_STOCK, article.getId(), article.getName(), 0L, e.getMessage());
        }
    }
}
