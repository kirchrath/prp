package home.inventory.device.ota;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface OtaJobRepository extends ListCrudRepository<OtaJob, UUID> {

    Optional<OtaJob> findByToken(String token);

    Optional<OtaJob> findFirstByScannerIdOrderByCreatedDateDesc(UUID scannerId);
}
