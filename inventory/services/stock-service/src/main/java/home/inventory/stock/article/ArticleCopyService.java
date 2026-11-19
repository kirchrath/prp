package home.inventory.stock.article;

import home.inventory.stock.article.dto.ArticleEventDto;
import home.inventory.stock.batch.StockService;
import home.inventory.stock.booking.BookingService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Maintains the local copy of catalog articles and reacts to new/merged articles. */
@Service
@Transactional
public class ArticleCopyService {

    private final ArticleCopyRepository copies;
    private final BarcodeCopyRepository barcodes;
    private final StockService stock;
    private final BookingService bookings;

    public ArticleCopyService(ArticleCopyRepository copies, BarcodeCopyRepository barcodes, StockService stock,
                              BookingService bookings) {
        this.copies = copies;
        this.barcodes = barcodes;
        this.stock = stock;
        this.bookings = bookings;
    }

    public void apply(ArticleEventDto event) {
        ArticleCopy copy = copies.findByArticleId(event.id()).orElseGet(() -> ArticleCopy.create(event.id()));
        long incoming = event.audit().version();
        if (incoming > copy.sourceVersion()) {
            copies.save(copy.withSource(incoming, event.audit().deleted(), event.data().name(), event.data().brand(),
                    event.data().imageUrl(), event.data().status()));
            syncBarcodes(event);
            if (event.data().mergedInto() != null) {
                stock.moveArticle(event.id(), event.data().mergedInto());
            }
        }
        // also on re-published (unchanged) articles: bookings may have arrived before the article
        if (!event.audit().deleted()) {
            for (String ean : event.data().eans()) {
                bookings.completePending(ean);
            }
        }
    }

    private void syncBarcodes(ArticleEventDto event) {
        Set<String> wanted = event.audit().deleted() ? new HashSet<>() : new HashSet<>(event.data().eans());
        List<BarcodeCopy> existing = barcodes.findAllByArticleIdAndDeletedFalse(event.id());
        for (BarcodeCopy b : existing) {
            if (!wanted.remove(b.ean())) {
                barcodes.save(b.withDeleted(true));
            }
        }
        for (String ean : wanted) {
            barcodes.save(BarcodeCopy.create(ean, event.id()));
        }
    }
}
