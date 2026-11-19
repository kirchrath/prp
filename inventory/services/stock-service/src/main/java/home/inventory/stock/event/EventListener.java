package home.inventory.stock.event;

import home.inventory.stock.article.ArticleCopyService;
import home.inventory.stock.article.dto.ArticleEventDto;
import home.inventory.stock.booking.BookingService;
import home.inventory.stock.booking.dto.ScanEventDto;
import home.inventory.stock.config.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * One listener (one consumer thread) for all incoming topics, so a scan and the article it waits for are normally
 * not processed concurrently. Retried messages run in parallel; a booking that stays pending because of that is
 * completed when the catalog publishes the article again (it does so for every pending booking).
 */
@Component
public class EventListener {

    private final ArticleCopyService articles;
    private final BookingService bookings;
    private final ObjectMapper json;

    public EventListener(ArticleCopyService articles, BookingService bookings, ObjectMapper json) {
        this.articles = articles;
        this.bookings = bookings;
        this.json = json;
    }

    @KafkaListener(topics = {Topics.CATALOG_ARTICLE, Topics.DEVICE_SCAN})
    void onEvent(String payload, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        // also called for the retry topics (see KafkaRetryConfig)
        String source = topic.endsWith(Topics.RETRY_SUFFIX) ? topic.substring(0, topic.length() - Topics.RETRY_SUFFIX.length()) : topic;
        if (Topics.CATALOG_ARTICLE.equals(source)) {
            articles.apply(json.readValue(payload, ArticleEventDto.class));
        } else {
            bookings.handleScan(json.readValue(payload, ScanEventDto.class));
        }
    }
}
