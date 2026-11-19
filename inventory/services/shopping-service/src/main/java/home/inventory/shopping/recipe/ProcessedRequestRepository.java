package home.inventory.shopping.recipe;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface ProcessedRequestRepository extends ListCrudRepository<ProcessedRequest, UUID> {

    boolean existsByRequestId(UUID requestId);
}
