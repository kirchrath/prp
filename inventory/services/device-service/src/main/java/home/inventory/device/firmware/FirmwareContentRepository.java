package home.inventory.device.firmware;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface FirmwareContentRepository extends ListCrudRepository<FirmwareContent, UUID> {

    Optional<FirmwareContent> findByFirmwareId(UUID firmwareId);
}
