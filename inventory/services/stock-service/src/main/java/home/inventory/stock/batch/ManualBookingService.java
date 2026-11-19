package home.inventory.stock.batch;

import home.inventory.stock.batch.dto.LocationLevelDto;
import home.inventory.stock.batch.dto.ManualBookingDto;
import home.inventory.stock.common.CurrentUser;
import home.inventory.stock.common.NotFoundException;
import home.inventory.stock.location.LocationService;
import home.inventory.stock.movement.BookingContext;
import home.inventory.stock.movement.MovementSource;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Manual bookings and batch maintenance from the web UI. */
@Service
@Transactional
public class ManualBookingService {

    private final StockService stock;
    private final LocationService locations;

    public ManualBookingService(StockService stock, LocationService locations) {
        this.stock = stock;
        this.locations = locations;
    }

    public LocationLevelDto bookIn(ManualBookingDto request) {
        requireLocation(request.locationId());
        long quantity = stock.stockIn(request.articleId(), request.locationId(), request.quantity(), request.bestBefore(), ui());
        return new LocationLevelDto(request.articleId(), request.locationId(), quantity);
    }

    public LocationLevelDto bookOut(ManualBookingDto request) {
        requireLocation(request.locationId());
        long quantity = stock.stockOut(request.articleId(), request.locationId(), request.quantity(), request.bestBefore(), ui());
        return new LocationLevelDto(request.articleId(), request.locationId(), quantity);
    }

    public void correct(UUID batchId, Long version, int quantity) {
        stock.correct(batchId, version, quantity, ui());
    }

    private void requireLocation(UUID locationId) {
        if (locations.findActive(locationId).isEmpty()) {
            throw new NotFoundException("Lagerort", locationId);
        }
    }

    private static BookingContext ui() {
        return new BookingContext(MovementSource.UI, null, CurrentUser.name(), null);
    }
}
