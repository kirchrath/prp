package home.inventory.scanner;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ScannerRepository extends JpaRepository<Scanner, Long> {

    Optional<Scanner> findByDeviceId(String deviceId);

    @Query("select s from Scanner s left join fetch s.location order by s.deviceId")
    List<Scanner> findAllOrdered();
}
