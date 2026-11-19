package home.inventory.device.scanner;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ScannerRepository extends ListCrudRepository<Scanner, UUID> {

    Optional<Scanner> findByDeviceId(String deviceId);

    List<Scanner> findAllByDeletedFalseOrderByDeviceIdAsc();
}
