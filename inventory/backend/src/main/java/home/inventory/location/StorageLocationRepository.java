package home.inventory.location;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    boolean existsByParent(StorageLocation parent);
}
