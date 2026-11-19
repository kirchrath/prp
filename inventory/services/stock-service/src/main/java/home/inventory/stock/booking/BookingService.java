package home.inventory.stock.booking;

import home.inventory.stock.article.ArticleCopy;
import home.inventory.stock.article.ArticleCopyRepository;
import home.inventory.stock.article.BarcodeCopyRepository;
import home.inventory.stock.batch.InsufficientStockException;
import home.inventory.stock.batch.StockService;
import home.inventory.stock.booking.dto.BookingDataDto;
import home.inventory.stock.booking.dto.BookingDto;
import home.inventory.stock.booking.dto.BookingEventDto;
import home.inventory.stock.booking.dto.ScanBookingDto;
import home.inventory.stock.booking.dto.ScanEventDto;
import home.inventory.stock.common.AuditMapper;
import home.inventory.stock.common.CurrentUser;
import home.inventory.stock.common.EventPublisher;
import home.inventory.stock.common.Gtin;
import home.inventory.stock.common.NotFoundException;
import home.inventory.stock.common.OutgoingEvent;
import home.inventory.stock.config.Topics;
import home.inventory.stock.location.LocationService;
import home.inventory.stock.movement.BookingContext;
import home.inventory.stock.movement.MovementSource;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Books scanned units (ESP32 scanners via device service, camera scan in the browser).
 * Unknown barcodes on store wait as {@link BookingStatus#PENDING_ARTICLE} until the catalog service has created
 * the article and the stock service received it.
 */
@Service
@Transactional(noRollbackFor = InsufficientStockException.class)
public class BookingService {

    static final String REQUESTED = "REQUESTED";

    private final BookingRepository repository;
    private final ArticleCopyRepository articles;
    private final BarcodeCopyRepository barcodes;
    private final LocationService locations;
    private final StockService stock;
    private final EventPublisher events;

    public BookingService(BookingRepository repository, ArticleCopyRepository articles, BarcodeCopyRepository barcodes,
                          LocationService locations, StockService stock, EventPublisher events) {
        this.repository = repository;
        this.articles = articles;
        this.barcodes = barcodes;
        this.locations = locations;
        this.stock = stock;
        this.events = events;
    }

    /** Camera scan from the browser. */
    public UUID scan(ScanBookingDto request) {
        return book(Booking.create(Gtin.require(request.ean()), request.locationId(), request.direction(), MovementSource.MOBILE,
                null, null, CurrentUser.name())).id();
    }

    /** Scan of an ESP32 scanner (topic {@code device_scan}); every scan is booked only once. */
    public void handleScan(ScanEventDto event) {
        var data = event.data();
        if (event.audit().deleted() || !REQUESTED.equals(data.status()) || data.ean() == null
                || data.locationId() == null || data.direction() == null || repository.existsByScanId(event.id())) {
            return;
        }
        Direction direction;
        try {
            direction = Direction.valueOf(data.direction());
        } catch (IllegalArgumentException e) {
            return;
        }
        Optional<String> ean = Gtin.normalize(data.ean());
        if (ean.isEmpty()) {
            save(Booking.create(data.ean().trim(), data.locationId(), direction, MovementSource.SCANNER, event.id(),
                    data.scannerId(), null).withResult(BookingStatus.INVALID_EAN, null, null, null, Gtin.INVALID));
            return;
        }
        book(Booking.create(ean.get(), data.locationId(), direction, MovementSource.SCANNER, event.id(),
                data.scannerId(), null));
    }

    @Transactional(readOnly = true)
    public BookingDto get(UUID id) {
        Booking b = repository.findById(id).orElseThrow(() -> new NotFoundException("Buchung", id));
        return new BookingDto(AuditMapper.toDto(b), data(b));
    }

    /** Completes bookings that waited for the article with this barcode. */
    public void completePending(String ean) {
        if (findByEan(ean).isEmpty()) {
            return;
        }
        for (Booking booking : repository.findAllByEanAndStatusAndDeletedFalse(ean, BookingStatus.PENDING_ARTICLE)) {
            save(process(booking, true));
        }
    }

    private Booking book(Booking booking) {
        // saved first (without event) so movements can refer to the booking id
        return save(process(repository.save(booking), false));
    }

    private Booking process(Booking booking, boolean articleWasPending) {
        if (locations.findActive(booking.locationId()).isEmpty()) {
            return booking.withResult(BookingStatus.NO_LOCATION, null, null, null, "Lagerort nicht vorhanden");
        }
        Optional<ArticleCopy> found = findByEan(booking.ean());
        if (found.isEmpty()) {
            return booking.direction() == Direction.IN
                    ? booking.withResult(BookingStatus.PENDING_ARTICLE, null, null, null, "Artikel wird angelegt …")
                    : booking.withResult(BookingStatus.UNKNOWN_EAN, null, null, null, "Unbekannter Barcode " + booking.ean());
        }
        ArticleCopy article = found.get();
        BookingContext context = new BookingContext(booking.source(), booking.scannerId(), booking.username(), booking.id());
        if (booking.direction() == Direction.IN) {
            long quantity = stock.stockIn(article.articleId(), booking.locationId(), 1, null, context);
            BookingStatus status = !articleWasPending ? BookingStatus.IN_OK
                    : ArticleCopy.UNKNOWN.equals(article.status()) ? BookingStatus.IN_OK_UNKNOWN_ARTICLE
                    : BookingStatus.IN_OK_NEW_ARTICLE;
            String message = status == BookingStatus.IN_OK_UNKNOWN_ARTICLE
                    ? "Unbekannter Artikel eingelagert – bitte in der Inbox ergänzen"
                    : article.name() + " eingelagert";
            return booking.withResult(status, article.articleId(), article.name(), quantity, message);
        }
        try {
            long quantity = stock.stockOut(article.articleId(), booking.locationId(), 1, null, context);
            return booking.withResult(BookingStatus.OUT_OK, article.articleId(), article.name(), quantity,
                    article.name() + " ausgelagert");
        } catch (InsufficientStockException e) {
            return booking.withResult(BookingStatus.NOT_IN_STOCK, article.articleId(), article.name(), 0L,
                    article.name() + ": " + e.getMessage());
        }
    }

    private Optional<ArticleCopy> findByEan(String ean) {
        // barcodes of deleted articles are deleted as well
        return barcodes.findFirstByEanAndDeletedFalse(ean).flatMap(b -> articles.findByArticleId(b.articleId()));
    }

    private Booking save(Booking booking) {
        Booking saved = repository.save(booking);
        events.publish(new OutgoingEvent(Topics.BOOKING, saved.id(),
                new BookingEventDto(saved.id(), AuditMapper.toDto(saved), data(saved))));
        return saved;
    }

    private static BookingDataDto data(Booking b) {
        return new BookingDataDto(b.ean(), b.locationId(), b.direction(), b.source(), b.scanId(), b.scannerId(),
                b.username(), b.status(), b.articleId(), b.articleName(), b.quantityAtLocation(), b.message());
    }
}
