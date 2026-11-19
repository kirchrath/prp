package home.inventory.stock.movement;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface MovementRepository extends ListCrudRepository<Movement, UUID> {
}
