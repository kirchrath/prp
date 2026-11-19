package home.inventory.stock.booking;

import home.inventory.stock.booking.dto.ScanEventDto;
import home.inventory.stock.config.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ScanListener {

    private final BookingService service;
    private final ObjectMapper json;

    public ScanListener(BookingService service, ObjectMapper json) {
        this.service = service;
        this.json = json;
    }

    @KafkaListener(topics = Topics.DEVICE_SCAN)
    void onScan(String payload) {
        service.handleScan(json.readValue(payload, ScanEventDto.class));
    }
}
