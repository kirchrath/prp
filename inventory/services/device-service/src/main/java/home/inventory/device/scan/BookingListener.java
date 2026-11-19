package home.inventory.device.scan;

import home.inventory.device.config.Topics;
import home.inventory.device.scan.dto.BookingEventDto;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class BookingListener {

    private final ScanService service;
    private final ObjectMapper json;

    public BookingListener(ScanService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Topics.STOCK_BOOKING)
    void onBooking(String payload) {
        service.handleBooking(json.readValue(payload, BookingEventDto.class));
    }
}
