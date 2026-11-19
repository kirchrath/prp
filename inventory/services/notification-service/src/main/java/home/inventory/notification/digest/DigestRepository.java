package home.inventory.notification.digest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;

public interface DigestRepository extends ListCrudRepository<Digest, UUID> {

    Optional<Digest> findByDay(LocalDate day);

    List<Digest> findTop14ByDeletedFalseOrderByDayDesc();
}
