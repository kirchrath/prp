package home.inventory.device.scan;

import home.inventory.device.common.AuditMapper;
import home.inventory.device.common.EventPublisher;
import home.inventory.device.common.OutgoingEvent;
import home.inventory.device.config.Topics;
import home.inventory.device.mqtt.dto.ScanMessageDto;
import home.inventory.device.scan.dto.BookingDataDto;
import home.inventory.device.scan.dto.BookingEventDto;
import home.inventory.device.scan.dto.ScanDataDto;
import home.inventory.device.scan.dto.ScanEventDto;
import home.inventory.device.scanner.Scanner;
import home.inventory.device.scanner.ScannerService;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Forwards scans of the ESP32 scanners to the stock service ({@code device_scan}) and reports the bookings
 * ({@code stock_booking}) back to the scanner.
 */
@Service
@Transactional
public class ScanService {

    static final Set<String> DIRECTIONS = Set.of("IN", "OUT");
    static final String PENDING_ARTICLE = "PENDING_ARTICLE";
    static final String REQUESTED = "REQUESTED";

    private final ScanRepository repository;
    private final ScannerService scanners;
    private final FeedbackService feedback;
    private final EventPublisher events;

    public ScanService(ScanRepository repository, ScannerService scanners, FeedbackService feedback,
                       EventPublisher events) {
        this.repository = repository;
        this.scanners = scanners;
        this.feedback = feedback;
        this.events = events;
    }

    public void handleScan(String deviceId, ScanMessageDto message) {
        Scanner scanner = scanners.register(deviceId);
        String direction = message.direction() == null ? "" : message.direction().trim().toUpperCase(Locale.ROOT);
        if (message.ean() == null || message.ean().isBlank() || !DIRECTIONS.contains(direction)
                || message.seq() == null || message.bootId() == null) {
            feedback.send(deviceId, message.seq(), FeedbackService.ERROR, null, null, "Ungültige Scan-Nachricht");
            return;
        }
        if (scanner.isDuplicate(message.bootId(), message.seq())) {
            return;
        }
        scanner = scanners.save(scanner.withLastScan(message.bootId(), message.seq(), Instant.now()));
        Scan scan = Scan.create(scanner.id(), deviceId, message.ean().trim(), direction, scanner.locationId(),
                message.seq(), message.bootId());
        if (scanner.locationId() == null) {
            String text = "Scanner " + scanner.displayName() + " ist keinem Lagerort zugeordnet";
            save(scan.withResult(ScanStatus.REJECTED, FeedbackService.NO_LOCATION, null, null, text, null));
            feedback.send(deviceId, message.seq(), FeedbackService.NO_LOCATION, null, null, text);
            return;
        }
        save(scan);
    }

    /** Result of the stock service for a scan of this service. */
    public void handleBooking(BookingEventDto event) {
        BookingDataDto data = event.data();
        if (data.scanId() == null || REQUESTED.equals(data.status())) {
            return;
        }
        repository.findById(data.scanId())
                .filter(scan -> scan.status() == ScanStatus.REQUESTED)
                .ifPresent(scan -> {
                    String message = data.message() == null ? "" : data.message();
                    if (!PENDING_ARTICLE.equals(data.status())) {
                        save(scan.withResult(ScanStatus.PROCESSED, data.status(), data.articleName(),
                                data.quantityAtLocation(), message, event.id()));
                    }
                    feedback.send(scan.deviceId(), scan.seq(), data.status(), data.articleName(),
                            data.quantityAtLocation(), message);
                });
    }

    private void save(Scan scan) {
        Scan saved = repository.save(scan);
        events.publish(new OutgoingEvent(Topics.SCAN, saved.id(), new ScanEventDto(saved.id(), AuditMapper.toDto(saved),
                new ScanDataDto(saved.scannerId(), saved.deviceId(), saved.ean(), saved.direction(), saved.locationId(),
                        saved.seq(), saved.bootId(), saved.status(), saved.result(), saved.articleName(),
                        saved.quantity(), saved.message(), saved.bookingId()))));
    }
}
