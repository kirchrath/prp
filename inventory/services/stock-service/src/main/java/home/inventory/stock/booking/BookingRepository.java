package home.inventory.stock.booking;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface BookingRepository extends ListCrudRepository<Booking, UUID> {

    boolean existsByScanId(UUID scanId);

    List<Booking> findAllByEanAndStatusAndDeletedFalse(String ean, BookingStatus status);
}
