package home.inventory.notification.copy;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

public interface BatchCopyRepository extends ListCrudRepository<BatchCopy, UUID> {

    Optional<BatchCopy> findByBatchId(UUID batchId);

    /** Batches in stock with a best-before date up to the given day. */
    @Query("select * from batch_copy where not deleted and quantity > 0 and best_before is not null"
            + " and best_before <= :until order by best_before")
    List<BatchCopy> findDueUntil(LocalDate until);
}
