package home.inventory.catalog.booking;

import home.inventory.catalog.article.ArticleService;
import home.inventory.catalog.booking.dto.BookingEventDto;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Reacts to stock bookings that wait for an article with an unknown barcode. */
@Service
public class BookingEventService {

    static final String PENDING_ARTICLE = "PENDING_ARTICLE";

    private final ArticleService articles;
    private final ObjectMapper json;

    public BookingEventService(ArticleService articles, ObjectMapper json) {
        this.articles = articles;
        this.json = json;
    }

    public void handle(String payload) {
        BookingEventDto event = json.readValue(payload, BookingEventDto.class);
        if (!event.audit().deleted() && PENDING_ARTICLE.equals(event.data().status()) && event.data().ean() != null) {
            articles.resolvePendingBarcode(event.data().ean());
        }
    }
}
