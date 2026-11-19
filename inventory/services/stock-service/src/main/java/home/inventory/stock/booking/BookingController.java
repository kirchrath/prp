package home.inventory.stock.booking;

import home.inventory.stock.booking.dto.BookingDto;
import home.inventory.stock.booking.dto.ScanBookingDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock/bookings")
@Tag(name = "Bookings")
public class BookingController {

    private final BookingService service;

    BookingController(BookingService service) {
        this.service = service;
    }

    /** Camera scan; the result may be pending until an unknown article was created (poll GET). */
    @PostMapping
    public ResponseEntity<Void> createBooking(@Valid @RequestBody ScanBookingDto request) {
        return ResponseEntity.created(URI.create("/api/stock/bookings/" + service.scan(request))).build();
    }

    @GetMapping("/{id}")
    public BookingDto getBooking(@PathVariable UUID id) {
        return service.get(id);
    }
}
