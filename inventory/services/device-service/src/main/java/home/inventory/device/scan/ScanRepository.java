package home.inventory.device.scan;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ScanRepository extends ListCrudRepository<Scan, UUID> {
}
