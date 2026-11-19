package home.inventory.location;

import home.inventory.common.ConflictException;
import home.inventory.common.NotFoundException;
import home.inventory.location.LocationDtos.LocationRequest;
import home.inventory.stock.StockBatchRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LocationService {

    private final StorageLocationRepository repository;
    private final StockBatchRepository batches;

    LocationService(StorageLocationRepository repository, StockBatchRepository batches) {
        this.repository = repository;
        this.batches = batches;
    }

    @Transactional(readOnly = true)
    public List<StorageLocation> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator.comparing(StorageLocation::getPath, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public StorageLocation get(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Lagerort", id));
    }

    /** Id of the location and all of its descendants. */
    @Transactional(readOnly = true)
    public Set<Long> subtreeIds(Long rootId) {
        StorageLocation root = get(rootId);
        return repository.findAll().stream()
                .filter(root::isSelfOrAncestorOf)
                .map(StorageLocation::getId)
                .collect(Collectors.toSet());
    }

    public StorageLocation create(LocationRequest request) {
        return repository.save(new StorageLocation(request.name().trim(), blankToNull(request.description()),
                parent(request.parentId())));
    }

    public StorageLocation update(Long id, LocationRequest request) {
        StorageLocation location = get(id);
        StorageLocation parent = parent(request.parentId());
        if (parent != null && location.isSelfOrAncestorOf(parent)) {
            throw new ConflictException("Ein Lagerort kann nicht unter sich selbst verschoben werden");
        }
        location.setName(request.name().trim());
        location.setDescription(blankToNull(request.description()));
        location.setParent(parent);
        return location;
    }

    public void delete(Long id) {
        StorageLocation location = get(id);
        if (repository.existsByParent(location)) {
            throw new ConflictException("Lagerort enthält noch Unter-Lagerorte");
        }
        if (batches.existsByLocation(location)) {
            throw new ConflictException("Lagerort enthält noch Bestand");
        }
        repository.delete(location);
    }

    private @Nullable StorageLocation parent(@Nullable Long parentId) {
        return parentId == null ? null : get(parentId);
    }

    private static @Nullable String blankToNull(@Nullable String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
