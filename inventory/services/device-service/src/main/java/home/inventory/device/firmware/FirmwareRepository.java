package home.inventory.device.firmware;

import home.inventory.device.scanner.DeviceKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface FirmwareRepository extends ListCrudRepository<Firmware, UUID> {

    List<Firmware> findAllByDeletedFalseOrderByCreatedDateDesc();

    Optional<Firmware> findFirstByKindAndDeletedFalseOrderByCreatedDateDesc(DeviceKind kind);

    boolean existsByKindAndFirmwareVersionAndDeletedFalse(DeviceKind kind, String firmwareVersion);
}
