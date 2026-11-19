package home.inventory.stock.location;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface LocationRepository extends ListCrudRepository<Location, UUID> {

    List<Location> findAllByDeletedFalse();

    boolean existsByParentIdAndDeletedFalse(UUID parentId);
}
