package home.inventory.catalog.booking;

import home.inventory.catalog.config.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class BookingListener {

    private final BookingEventService service;

    public BookingListener(BookingEventService service) {
        this.service = service;
    }

    @KafkaListener(topics = Topics.STOCK_BOOKING)
    void onBooking(String payload) {
        service.handle(payload);
    }
}
